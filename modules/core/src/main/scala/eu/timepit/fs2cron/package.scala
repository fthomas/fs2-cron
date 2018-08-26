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

package eu.timepit

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import cats.effect.Sync
import cron4s.expr.CronExpr
import cron4s.lib.javatime._
import fs2.{Scheduler, Stream}

import scala.concurrent.duration.FiniteDuration

package object fs2cron {

  implicit def toSchedulerCronOps(scheduler: Scheduler): SchedulerCronOps =
    new SchedulerCronOps(scheduler)

  /** Calculates the duration between `from` and the next date-time
    * that matches `cronExpr`.
    */
  def durationFrom(from: LocalDateTime, cronExpr: CronExpr): Either[Throwable, FiniteDuration] =
    cronExpr.next(from) match {
      case Some(next) =>
        val durationInMillis = from.until(next, ChronoUnit.MILLIS)
        Right(FiniteDuration(durationInMillis, TimeUnit.MILLISECONDS))
      case None =>
        val msg = s"Could not calculate the next date-time from $from " +
          s"given the cron expression '$cronExpr'. This should never happen."
        Left(new Throwable(msg))
    }

  /** Creates a single element stream of the duration between the
    * current date-time and the next date-time that matches `cronExpr`.
    */
  def durationFromNow[F[_]: Sync](cronExpr: CronExpr): Stream[F, FiniteDuration] =
    evalNow.flatMap { now =>
      durationFrom(now, cronExpr) match {
        case Right(d) => Stream.emit(d)
        case Left(t)  => Stream.raiseError(t)
      }
    }

  /** Creates a single element stream of the current date-time. */
  def evalNow[F[_]](implicit F: Sync[F]): Stream[F, LocalDateTime] =
    Stream.eval(F.delay(LocalDateTime.now))

}
