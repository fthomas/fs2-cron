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

import cats.effect.{Sync, Temporal}
import com.github.eikek.calev.CalEvent
import eu.timepit.fs2cron.{Scheduler, ZonedDateTimeScheduler}

import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

object CalevScheduler {
  def systemDefault[F[_]](implicit temporal: Temporal[F], F: Sync[F]): Scheduler[F, CalEvent] =
    from(F.delay(ZoneId.systemDefault()))

  def utc[F[_]](implicit F: Temporal[F]): Scheduler[F, CalEvent] =
    from(F.pure(ZoneOffset.UTC))

  def from[F[_]](zoneId: F[ZoneId])(implicit F: Temporal[F]): Scheduler[F, CalEvent] =
    new ZonedDateTimeScheduler[F, CalEvent](zoneId) {
      override def next(from: ZonedDateTime, schedule: CalEvent): F[ZonedDateTime] =
        schedule.nextElapse(from) match {
          case Some(next) => F.pure(next)
          case None =>
            val msg = s"Could not calculate the next date-time from $from " +
              s"given the calendar event expression '${schedule.asString}'. This should never happen."
            F.raiseError(new Throwable(msg))
        }
    }
}
