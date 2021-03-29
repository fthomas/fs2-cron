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

import cats.effect.Concurrent
import fs2.Stream

final class ScheduledStreams[F[_], Schedule](scheduler: Scheduler[F, Schedule]) {
  def sleep(schedule: Schedule): Stream[F, Unit] =
    Stream.eval(scheduler.sleepUntilNext(schedule))

  def awakeEvery(schedule: Schedule): Stream[F, Unit] =
    sleep(schedule).repeat

  def schedule[A](tasks: List[(Schedule, Stream[F, A])])(implicit
      F: Concurrent[F]
  ): Stream[F, A] = {
    val scheduled = tasks.map { case (schedule, task) => awakeEvery(schedule) >> task }
    Stream.emits(scheduled).covary[F].parJoinUnbounded
  }
}
