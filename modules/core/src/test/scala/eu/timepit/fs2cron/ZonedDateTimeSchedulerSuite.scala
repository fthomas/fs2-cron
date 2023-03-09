package eu.timepit.fs2cron

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import java.time.{Instant, ZoneId, ZoneOffset}

trait ZonedDateTimeSchedulerSuite[Schedule] extends CatsEffectSuite {
  def schedulerCompanion: ZonedDateTimeScheduler.Companion[Schedule]
  def everySecond: Schedule
  def evenSeconds: Schedule

  private def isEven(i: Long): Boolean = i % 2 == 0
  private def instantSeconds(i: Instant): Long = i.getEpochSecond
  private val evalInstantNow: Stream[IO, Instant] = Stream.eval(IO(Instant.now()))

  private val schedulerSys = schedulerCompanion.systemDefault[IO]
  private val schedulerUtc = schedulerCompanion.utc[IO]

  test("awakeEvery") {
    val s1 = schedulerSys.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(isEven)
    assertIO(s2.compile.last, Some(true))
  }

  test("sleep") {
    val s1 = schedulerUtc.sleep(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).forall(isEven)
    assertIO(s2.compile.last, Some(true))
  }

  test("schedule") {
    val s1 = schedulerSys
      .schedule(List(everySecond -> evalInstantNow, evenSeconds -> evalInstantNow))
      .map(instantSeconds)

    for {
      seconds <- s1.take(3).compile.toList
      _ = assertEquals(seconds.count(isEven), 2)
      _ = assertEquals(seconds.count(!isEven(_)), 1)
    } yield ()
  }

  test("timezones") {
    val zoneId: ZoneId = ZoneOffset.ofTotalSeconds(1)
    val scheduler = schedulerCompanion.from(IO.pure(zoneId))

    val s1 = scheduler.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(!isEven(_))
    assertIO(s2.compile.last, Some(true))
  }
}
