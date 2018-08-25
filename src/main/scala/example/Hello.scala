package example

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import cats.effect.{Async, IO, Sync}
import cron4s._
import cron4s.expr.CronExpr
import cron4s.lib.javatime._
import fs2.{Scheduler, Stream}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Hello extends App {
  {
    import scala.concurrent.ExecutionContext.Implicits._

    val cronExpr = Cron.unsafeParse("1-5,10-15,20-25,30-35,40-45,50,52,55,58 * * ? * *")

    Scheduler[IO](4)
      .flatMap(scheduled(_, cronExpr, IO(println(LocalDateTime.now))))
      .take(40)
      .compile
      .drain
      .unsafeRunSync()
  }
  /*
Example output:

2018-08-24T18:42:45.004
2018-08-24T18:42:50.006
2018-08-24T18:42:52.004
2018-08-24T18:42:55.003
2018-08-24T18:42:58.004
2018-08-24T18:43:01.008
2018-08-24T18:43:02.005
2018-08-24T18:43:03.006
2018-08-24T18:43:04.005
2018-08-24T18:43:05.004
2018-08-24T18:43:10.004
2018-08-24T18:43:11.005
2018-08-24T18:43:12.004
2018-08-24T18:43:13.005
2018-08-24T18:43:14.005
2018-08-24T18:43:15.006
2018-08-24T18:43:20.005
   */

  def scheduled[F[_]: Async, O](scheduler: Scheduler, cronExpr: CronExpr, fo: F[O])(
      implicit ec: ExecutionContext
  ): Stream[F, O] = {
    def loop: Stream[F, O] =
      evalNow.flatMap { now =>
        durationUntilNext(cronExpr, now) match {
          case Some(d) => scheduler.sleep_(d) ++ Stream.eval(fo) ++ loop
          case None    => Stream.empty
        }
      }
    loop
  }

  def evalNow[F[_]](implicit F: Sync[F]): Stream[F, LocalDateTime] =
    Stream.eval(F.delay(LocalDateTime.now))

  def durationUntilNext(cronExpr: CronExpr, from: LocalDateTime): Option[FiniteDuration] =
    cronExpr.next(from).map { next =>
      val durationInMillis = from.until(next, ChronoUnit.MILLIS)
      FiniteDuration(durationInMillis, TimeUnit.MILLISECONDS)
    }
}
