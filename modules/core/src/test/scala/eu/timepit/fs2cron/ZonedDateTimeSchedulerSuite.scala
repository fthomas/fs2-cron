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

import java.time.{Instant, ZoneId, ZoneOffset}

trait ZonedDateTimeSchedulerSuite[Schedule] extends CatsEffectSuite {
  def schedulerCompanion: ZonedDateTimeScheduler.Companion[Schedule]
  def everySecond: Schedule
  def evenSeconds: Schedule

  private def isEven(i: BigDecimal): Boolean = {
    val result = i.rounded.toLong % 2 == 0
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
    println("awakeEvery")
    val s1 = schedulerSys.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(isEven)
    assertIO(s2.compile.last, Some(true))
  }

  test("sleep") {
    println("sleep")
    val s1 = schedulerUtc.sleep(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).forall(isEven)
    assertIO(s2.compile.last, Some(true))
  }

  test("schedule") {
    println("schedule")
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
    println("timezones")
    val zoneId: ZoneId = ZoneOffset.ofTotalSeconds(1)
    val scheduler = schedulerCompanion.from(IO.pure(zoneId))

    val s1 = scheduler.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(!isEven(_))
    assertIO(s2.compile.last, Some(true))
  }
}
