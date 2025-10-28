/*
 * Copyright 2018-2023 fs2-cron contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.timepit.fs2cron

import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait ZonedDateTimeSchedulerSuite[Schedule] extends CatsEffectSuite {
  def schedulerCompanion: ZonedDateTimeScheduler.Companion[Schedule]
  def everySecond: Schedule
  def evenSeconds: Schedule

  private def isEven(i: BigDecimal): Boolean = {
    val result = i.setScale(0, BigDecimal.RoundingMode.HALF_UP) % 2 == 0
    println(s"isEven($i) = $result")
    result
  }

  private def instantSeconds(i: Instant): BigDecimal = {
    val fraction = i.getNano / 1e9
    BigDecimal(i.getEpochSecond) + fraction
  }

  private val evalInstantNow: Stream[IO, Instant] =
    Stream.eval(IO.realTime.map(d => Instant.EPOCH.plusNanos(d.toNanos)).debug())

  private val schedulerSys = schedulerCompanion.systemDefault[IO]
  private val schedulerUtc = schedulerCompanion.utc[IO]

  test("awakeEvery") {
    val s1 = schedulerSys.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).drop(1).take(2).forall(isEven)
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

  test("durationUntilNext: from + duration >= next") {
    val scheduler = schedulerUtc.asInstanceOf[ZonedDateTimeScheduler[IO, Schedule]]

    val from = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 100000, ZoneOffset.UTC)
    val duration = scheduler.durationUntilNext(from, everySecond)

    val next = scheduler.next(from, everySecond)
    val fromPlusDuration = duration.map(d => from.plusNanos(d.toNanos))
    val fromPlusDurationGtEqNext = fromPlusDuration.flatMap(fpd => next.map(n => !fpd.isBefore(n)))

    for {
      _ <- assertIO(duration, FiniteDuration(1000L, TimeUnit.MILLISECONDS))
      _ <- assertIOBoolean(fromPlusDurationGtEqNext)
    } yield ()
  }
}
