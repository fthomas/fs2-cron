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

import cats.Monad
import cats.effect.{Sync, Timer}

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit.MILLISECONDS

trait TimezoneContext[F[_]] {
  def zoneId: F[ZoneId]

  def now(implicit F: Monad[F], timer: Timer[F]): F[ZonedDateTime] =
    F.flatMap(zoneId)(zone =>
      F.map(timer.clock.realTime(MILLISECONDS))(Instant.ofEpochMilli(_).atZone(zone))
    )
}

object TimezoneContext {
  implicit def systemDefault[F[_]](implicit F: Sync[F]): TimezoneContext[F] =
    TimezoneContext[F](F.delay(ZoneId.systemDefault()))

  implicit def utc[F[_]](implicit F: Sync[F]): TimezoneContext[F] =
    TimezoneContext[F](F.delay(ZoneOffset.UTC))

  def apply[F[_]](zone: F[ZoneId]): TimezoneContext[F] =
    new TimezoneContext[F] {
      override def zoneId: F[ZoneId] = zone
    }
}
