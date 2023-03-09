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

package eu.timepit.fs2cron.cronutils

import cats.effect.Temporal
import com.cronutils.model.time.ExecutionTime
import eu.timepit.fs2cron.{Scheduler, ZonedDateTimeScheduler}

import java.time.{ZoneId, ZonedDateTime}

object CronUtilsScheduler extends ZonedDateTimeScheduler.Companion[ExecutionTime] {
  override def from[F[_]](zoneId: F[ZoneId])(implicit F: Temporal[F]): Scheduler[F, ExecutionTime] =
    new ZonedDateTimeScheduler[F, ExecutionTime](zoneId) {
      override def next(from: ZonedDateTime, schedule: ExecutionTime): F[ZonedDateTime] =
        schedule.nextExecution(from).map[F[ZonedDateTime]](zdt => F.pure(zdt)).orElse {
          val msg = s"Could not calculate the next date-time from $from " +
            s"given the cron expression '$schedule'."
          F.raiseError(new Throwable(msg))
        }
    }
}
