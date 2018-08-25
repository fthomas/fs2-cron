package eu.timepit.fs2cron

import cats.effect.Async
import cron4s.expr.CronExpr
import fs2.{Scheduler, Stream}

import scala.concurrent.ExecutionContext

final class SchedulerCronOps(val scheduler: Scheduler) extends AnyVal {

  def awakeEveryCron[F[_]: Async](cronExpr: CronExpr)(
      implicit ec: ExecutionContext
  ): Stream[F, Unit] =
    sleepCron(cronExpr).repeat

  def sleepCron[F[_]: Async](cronExpr: CronExpr)(implicit ec: ExecutionContext): Stream[F, Unit] =
    durationFromNow(cronExpr).flatMap(d => scheduler.sleep(d))

}
