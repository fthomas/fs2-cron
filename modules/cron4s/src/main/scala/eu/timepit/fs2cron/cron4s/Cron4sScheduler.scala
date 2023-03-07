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

package eu.timepit.fs2cron.cron4s

import cats.effect.{Sync, Temporal}
import cron4s.expr.CronExpr
import cron4s.lib.javatime._
import cron4s.syntax.cron._
import eu.timepit.fs2cron.{Scheduler, ZonedDateTimeScheduler}

import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

object Cron4sScheduler {
  def systemDefault[F[_]](implicit temporal: Temporal[F], F: Sync[F]): Scheduler[F, CronExpr] =
    from(F.delay(ZoneId.systemDefault()))

  def utc[F[_]](implicit F: Temporal[F]): Scheduler[F, CronExpr] =
    from(F.pure(ZoneOffset.UTC))

  def from[F[_]](zoneId: F[ZoneId])(implicit F: Temporal[F]): Scheduler[F, CronExpr] =
    new ZonedDateTimeScheduler[F, CronExpr](zoneId) {
      override def next(from: ZonedDateTime, schedule: CronExpr): F[ZonedDateTime] =
        schedule.next(from) match {
          case Some(next) => F.pure(next)
          case None =>
            val msg = s"Could not calculate the next date-time from $from " +
              s"given the cron expression '$schedule'. This should never happen."
            F.raiseError(new Throwable(msg))
        }
    }
}
