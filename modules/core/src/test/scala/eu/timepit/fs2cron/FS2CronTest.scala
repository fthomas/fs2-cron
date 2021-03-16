package eu.timepit.fs2cron

import cats.effect.{ContextShift, IO, Timer}
import cron4s.Cron
import cron4s.expr.CronExpr
import fs2.Stream
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZoneOffset}
import scala.concurrent.ExecutionContext

class FS2CronTest extends AnyFunSuite with Matchers {
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  val evenSeconds: CronExpr = Cron.unsafeParse("*/2 * * ? * *")
  def isEven(i: Long): Boolean = i % 2 == 0
  def instantSeconds(i: Instant): Long = i.getEpochSecond
  val evalInstantNow: Stream[IO, Instant] = Stream.eval(IO(Instant.now()))

  test("awakeEveryCron") {
    import ZonedTimer.systemDefault

    val s1 = awakeEveryCron[IO](evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(isEven)
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }

  test("sleepCron") {
    import ZonedTimer.utc

    val s1 = sleepCron[IO](evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).forall(isEven)
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }

  test("schedule") {
    import ZonedTimer.systemDefault

    implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    val everySecond: CronExpr = Cron.unsafeParse("* * * ? * *")
    val s1 =
      schedule(List(everySecond -> evalInstantNow, evenSeconds -> evalInstantNow))
        .map(instantSeconds)

    val testIO = for {
      seconds <- s1.take(3).compile.toList
    } yield {
      seconds.count(isEven) shouldBe 2
      seconds.count(!isEven(_)) shouldBe 1
    }

    testIO.unsafeRunSync()
  }

  test("timezones") {
    implicit val zt: ZonedTimer[IO] =
      ZonedTimer.from(IO.pure[ZoneId](ZoneOffset.ofTotalSeconds(1)))

    val s1 = awakeEveryCron[IO](evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(!isEven(_))
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }
}
