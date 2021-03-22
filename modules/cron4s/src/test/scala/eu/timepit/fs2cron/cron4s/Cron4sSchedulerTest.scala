package eu.timepit.fs2cron.cron4s

import cats.effect.{ContextShift, IO, Timer}
import cron4s.Cron
import cron4s.expr.CronExpr
import eu.timepit.fs2cron.ScheduledStreams
import fs2.Stream
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZoneOffset}
import scala.concurrent.ExecutionContext

class Cron4sSchedulerTest extends AnyFunSuite with Matchers {
  private implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  private val evenSeconds: CronExpr = Cron.unsafeParse("*/2 * * ? * *")
  private def isEven(i: Long): Boolean = i % 2 == 0
  private def instantSeconds(i: Instant): Long = i.getEpochSecond
  private val evalInstantNow: Stream[IO, Instant] = Stream.eval(IO(Instant.now()))

  private val streamsSystemDefault: ScheduledStreams[IO, CronExpr] =
    new ScheduledStreams(Cron4sScheduler.systemDefault[IO])

  private val streamsUtc: ScheduledStreams[IO, CronExpr] =
    new ScheduledStreams(Cron4sScheduler.utc[IO])

  test("awakeEvery") {
    val s1 = streamsSystemDefault.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(isEven)
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }

  test("sleep") {
    val s1 = streamsUtc.sleep(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).forall(isEven)
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }

  test("schedule") {
    implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    val everySecond: CronExpr = Cron.unsafeParse("* * * ? * *")
    val s1 = streamsSystemDefault
      .schedule(List(everySecond -> evalInstantNow, evenSeconds -> evalInstantNow))
      .map(instantSeconds)

    val testIO = s1.take(3).compile.toList.map { seconds =>
      seconds.count(isEven) shouldBe 2
      seconds.count(!isEven(_)) shouldBe 1
    }

    testIO.unsafeRunSync()
  }

  test("timezones") {
    val zoneId: ZoneId = ZoneOffset.ofTotalSeconds(1)
    val streams = new ScheduledStreams(Cron4sScheduler.from(IO.pure(zoneId)))

    val s1 = streams.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(!isEven(_))
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }
}
