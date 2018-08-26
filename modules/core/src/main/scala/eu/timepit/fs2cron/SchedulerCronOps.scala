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

import cats.effect.Async
import cron4s.expr.CronExpr
import fs2.{Scheduler, Stream}

import scala.concurrent.ExecutionContext

/** Provides extension methods for `fs2.Scheduler` that works with
  * `cron4s.expr.CronExpr`.
  */
final class SchedulerCronOps(val scheduler: Scheduler) extends AnyVal {

  /** Creates a discrete stream that emits unit at every date-time from
    * now that matches `cronExpr`.
    */
  def awakeEveryCron[F[_]: Async](cronExpr: CronExpr)(
      implicit ec: ExecutionContext
  ): Stream[F, Unit] =
    sleepCron(cronExpr).repeat

  /** Creates a single element stream that waits until the next
    * date-time that matches `cronExpr` before emitting unit.
    */
  def sleepCron[F[_]: Async](cronExpr: CronExpr)(implicit ec: ExecutionContext): Stream[F, Unit] =
    durationFromNow(cronExpr).flatMap(d => scheduler.sleep(d))

}
