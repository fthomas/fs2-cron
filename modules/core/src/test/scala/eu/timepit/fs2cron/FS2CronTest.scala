package eu.timepit.fs2cron

import cats.effect.{ContextShift, IO, Timer}
import cron4s.Cron
import cron4s.expr.CronExpr
import scala.concurrent.ExecutionContext
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FS2CronTest extends AnyFunSuite with Matchers {
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  val evenSeconds: CronExpr = Cron.unsafeParse("*/2 * * ? * *")
  def isEven(i: Int): Boolean = i % 2 == 0

  test("awakeEveryCron") {
    val s1 = awakeEveryCron[IO](evenSeconds) >> evalNow[IO]
    val s2 = s1.map(_.getSecond).take(2).forall(isEven)
    s2.compile.last.map(_.getOrElse(false)).unsafeRunSync()
  }

  test("sleepCron") {
    val s1 = sleepCron[IO](evenSeconds) >> evalNow[IO]
    val s2 = s1.map(_.getSecond).forall(isEven)
    s2.compile.last.map(_.getOrElse(false)).unsafeRunSync()
  }

  test("schedule") {
    implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    val everySecond: CronExpr = Cron.unsafeParse("* * * ? * *")
    val s1 = schedule(List(everySecond -> evalNow[IO], evenSeconds -> evalNow[IO])).map(_.getSecond)

    val seconds = s1.take(3).compile.toList.unsafeRunSync()
    seconds.count(isEven) shouldBe 2
    seconds.count(!isEven(_)) shouldBe 1
  }
}
