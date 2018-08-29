package eu.timepit.fs2cron

import cats.effect.{IO, Timer}
import cron4s.Cron
import cron4s.expr.CronExpr
import org.scalatest.{FunSuite, Matchers}

class FS2CronTest extends FunSuite with Matchers {

  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
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

}
