package eu.timepit.fs2cron.cron4s

import cats.MonadThrow
import cats.effect.{Sync, Timer}
import cats.syntax.all._
import cron4s.expr.CronExpr
import cron4s.lib.javatime._
import eu.timepit.fs2cron.Scheduler

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration.FiniteDuration

object Cron4sScheduler {
  def systemDefault[F[_]](implicit timer: Timer[F], F: Sync[F]): Scheduler[F, CronExpr] =
    from(F.delay(ZoneId.systemDefault()))

  def utc[F[_]](implicit timer: Timer[F], F: MonadThrow[F]): Scheduler[F, CronExpr] =
    from(F.pure(ZoneOffset.UTC))

  def from[F[_]](zoneId: F[ZoneId])(implicit
      timer: Timer[F],
      F: MonadThrow[F]
  ): Scheduler[F, CronExpr] = {
    val timer0 = timer
    new Scheduler[F, CronExpr] {
      override def untilNext(schedule: CronExpr): F[FiniteDuration] =
        now.flatMap { from =>
          schedule.next(from) match {
            case Some(next) =>
              val durationInMillis = from.until(next, ChronoUnit.MILLIS)
              F.pure(FiniteDuration(durationInMillis, TimeUnit.MILLISECONDS))
            case None =>
              val msg = s"Could not calculate the next date-time from $from " +
                s"given the cron expression '$schedule'. This should never happen."
              F.raiseError(new Throwable(msg))
          }
        }

      override def timer: Timer[F] = timer0

      private val now: F[ZonedDateTime] =
        for {
          zoneId <- zoneId
          epochMilli <- timer0.clock.realTime(MILLISECONDS)
        } yield Instant.ofEpochMilli(epochMilli).atZone(zoneId)
    }
  }
}
