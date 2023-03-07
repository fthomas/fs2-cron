/*
 * Copyright 2018-2021 fs2-cron contributors
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

package eu.timepit.fs2cron.calev

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.eikek.calev.CalEvent
import fs2.Stream
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZoneOffset}

class CalevSchedulerTest extends AnyFunSuite with Matchers {
  private val evenSeconds: CalEvent = CalEvent.unsafe("*-*-* *:*:0/2")
  private def isEven(i: Long): Boolean = i % 2 == 0
  private def instantSeconds(i: Instant): Long = i.getEpochSecond
  private val evalInstantNow: Stream[IO, Instant] = Stream.eval(IO(Instant.now()))

  private val schedulerSys = CalevScheduler.systemDefault[IO]
  private val schedulerUtc = CalevScheduler.utc[IO]

  test("awakeEvery") {
    val s1 = schedulerSys.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(isEven)
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }

  test("sleep") {
    val s1 = schedulerUtc.sleep(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).forall(isEven)
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }

  test("schedule") {
    val everySecond: CalEvent = CalEvent.unsafe("*-*-* *:*:*")
    val s1 = schedulerSys
      .schedule(List(everySecond -> evalInstantNow, evenSeconds -> evalInstantNow))
      .map(instantSeconds)

    (for {
      seconds <- s1.take(3).compile.toList
      _ <- IO(seconds.count(isEven) shouldBe 2)
      _ <- IO(seconds.count(!isEven(_)) shouldBe 1)
    } yield ()).unsafeRunSync()
  }

  test("timezones") {
    val zoneId: ZoneId = ZoneOffset.ofTotalSeconds(1)
    val scheduler = CalevScheduler.from(IO.pure(zoneId))

    val s1 = scheduler.awakeEvery(evenSeconds) >> evalInstantNow
    val s2 = s1.map(instantSeconds).take(2).forall(!isEven(_))
    s2.compile.last.map(_ should be(Option(true))).unsafeRunSync()
  }
}
