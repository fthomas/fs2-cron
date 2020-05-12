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

import cats.ApplicativeError
import cats.effect.{Concurrent, Sync, Timer}
import cron4s.expr.CronExpr
import cron4s.lib.javatime._
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

package object fs2cron {

  /** Creates a discrete stream that emits unit at every date-time from
    * now that matches `cronExpr`.
    */
  def awakeEveryCron[F[_]: Sync](cronExpr: CronExpr)(implicit timer: Timer[F]): Stream[F, Unit] =
    sleepCron(cronExpr).repeat

  /** Creates a single element stream of the duration between `from`
    * and the next date-time that matches `cronExpr`.
    */
  def durationFrom[F[_]](from: LocalDateTime, cronExpr: CronExpr)(implicit
      F: ApplicativeError[F, Throwable]
  ): Stream[F, FiniteDuration] =
    cronExpr.next(from) match {
      case Some(next) =>
        val durationInMillis = from.until(next, ChronoUnit.MILLIS)
        Stream.emit(FiniteDuration(durationInMillis, TimeUnit.MILLISECONDS))
      case None =>
        val msg = s"Could not calculate the next date-time from $from " +
          s"given the cron expression '$cronExpr'. This should never happen."
        Stream.raiseError(new Throwable(msg))
    }

  /** Creates a single element stream of the duration between the
    * current date-time and the next date-time that matches `cronExpr`.
    */
  def durationFromNow[F[_]: Sync](cronExpr: CronExpr): Stream[F, FiniteDuration] =
    evalNow.flatMap(now => durationFrom(now, cronExpr))

  /** Creates a single element stream of the current date-time. */
  def evalNow[F[_]](implicit F: Sync[F]): Stream[F, LocalDateTime] =
    Stream.eval(F.delay(LocalDateTime.now))

  /** Creates a single element stream that waits until the next
    * date-time that matches `cronExpr` before emitting unit.
    */
  def sleepCron[F[_]: Sync](cronExpr: CronExpr)(implicit timer: Timer[F]): Stream[F, Unit] =
    durationFromNow(cronExpr).flatMap(Stream.sleep[F])

  def schedule[F[_]: Concurrent, A](tasks: List[(CronExpr, Stream[F, A])])(implicit
      timer: Timer[F]
  ): Stream[F, A] = {
    val scheduled = tasks.map { case (cronExpr, task) => awakeEveryCron[F](cronExpr) >> task }
    Stream.emits(scheduled).covary[F].parJoinUnbounded
  }
}
