/*
 * Copyright 2018 fs2-cron contributors
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

import cats.effect.{Sync, Timer}
import cats.syntax.all._
import cats.{FlatMap, Monad}

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit.MILLISECONDS

trait ZonedTimer[F[_]] {
  def now: F[ZonedDateTime]

  def timer: Timer[F]
}

object ZonedTimer {
  implicit def systemDefault[F[_]](implicit timer: Timer[F], F: Sync[F]): ZonedTimer[F] =
    from(F.delay(ZoneId.systemDefault()))

  implicit def utc[F[_]](implicit timer: Timer[F], F: Monad[F]): ZonedTimer[F] =
    from(F.pure(ZoneOffset.UTC))

  def from[F[_]](zoneId: F[ZoneId])(implicit timer: Timer[F], F: FlatMap[F]): ZonedTimer[F] = {
    val timer0 = timer
    new ZonedTimer[F] {
      override val now: F[ZonedDateTime] =
        for {
          zoneId <- zoneId
          epochMilli <- timer0.clock.realTime(MILLISECONDS)
        } yield Instant.ofEpochMilli(epochMilli).atZone(zoneId)

      override val timer: Timer[F] = timer0
    }
  }
}
