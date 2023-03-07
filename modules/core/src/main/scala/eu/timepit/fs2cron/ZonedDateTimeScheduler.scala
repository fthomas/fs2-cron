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

package eu.timepit.fs2cron

import cats.effect.Temporal
import cats.syntax.all._

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

abstract class ZonedDateTimeScheduler[F[_], Schedule](zoneId: F[ZoneId])(implicit
    override val temporal: Temporal[F]
) extends Scheduler[F, Schedule] {
  def next(from: ZonedDateTime, schedule: Schedule): F[ZonedDateTime]

  override def fromNowUntilNext(schedule: Schedule): F[FiniteDuration] =
    now.flatMap { from =>
      next(from, schedule).map { to =>
        val durationInMillis = from.until(to, ChronoUnit.MILLIS)
        FiniteDuration(durationInMillis, TimeUnit.MILLISECONDS)
      }
    }

  private val now: F[ZonedDateTime] =
    (temporal.realTimeInstant, zoneId).mapN(_.atZone(_))
}
