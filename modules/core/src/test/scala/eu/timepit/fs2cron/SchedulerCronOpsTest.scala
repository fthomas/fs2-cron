package eu.timepit.fs2cron

import cats.effect.IO
import cron4s.Cron
import cron4s.expr.CronExpr
import fs2.{Scheduler, Stream}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits._

class SchedulerCronOpsTest extends FunSuite with Matchers {

  val scheduler: Stream[IO, Scheduler] = Scheduler[IO](1)
  val evenSeconds: CronExpr = Cron.unsafeParse("*/2 * * ? * *")

  test("sleepCron") {
    val s = scheduler.flatMap(_.sleepCron[IO](evenSeconds)) >> evalNow[IO]
    s.compile.last.map(_.forall(_.getSecond % 2 == 0)).unsafeRunSync()
  }

}
