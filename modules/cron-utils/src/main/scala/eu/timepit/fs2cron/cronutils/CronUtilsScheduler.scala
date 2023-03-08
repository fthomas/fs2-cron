package eu.timepit.fs2cron.cronutils

import cats.effect.{Sync, Temporal}
import com.cronutils.model.time.ExecutionTime
import eu.timepit.fs2cron.{Scheduler, ZonedDateTimeScheduler}

import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

object CronUtilsScheduler {
  def systemDefault[F[_]](implicit temporal: Temporal[F], F: Sync[F]): Scheduler[F, ExecutionTime] =
    from(F.delay(ZoneId.systemDefault()))

  def utc[F[_]](implicit F: Temporal[F]): Scheduler[F, ExecutionTime] =
    from(F.pure(ZoneOffset.UTC))

  def from[F[_]](zoneId: F[ZoneId])(implicit F: Temporal[F]): Scheduler[F, ExecutionTime] =
    new ZonedDateTimeScheduler[F, ExecutionTime](zoneId) {
      override def next(from: ZonedDateTime, schedule: ExecutionTime): F[ZonedDateTime] =
        schedule.nextExecution(from).map(zdt => F.pure(zdt)).orElse {
          val msg = s"Could not calculate the next date-time from $from " +
            s"given the cron expression '$schedule'."
          F.raiseError(new Throwable(msg))
        }
    }
}
