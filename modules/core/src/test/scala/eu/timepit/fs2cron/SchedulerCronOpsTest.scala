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
  def isEven(i: Int): Boolean = i % 2 == 0

  test("awakeEveryCron") {
    val s1 = scheduler.flatMap(_.awakeEveryCron[IO](evenSeconds)) >> evalNow[IO]
    val s2 = s1.map(_.getSecond).take(2).forall(isEven)
    s2.compile.last.map(_.getOrElse(false)).unsafeRunSync()
  }

  test("sleepCron") {
    val s1 = (scheduler.flatMap(_.sleepCron[IO](evenSeconds)) >> evalNow[IO])
    val s2 = s1.map(_.getSecond).forall(isEven)
    s2.compile.last.map(_.getOrElse(false)).unsafeRunSync()
  }

}
