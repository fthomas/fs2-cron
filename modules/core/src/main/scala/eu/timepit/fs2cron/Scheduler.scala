/*
 * Copyright 2018-2023 fs2-cron contributors
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
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

trait Scheduler[F[_], Schedule] {
  def fromNowUntilNext(schedule: Schedule): F[FiniteDuration]
  def temporal: Temporal[F]

  final def sleepUntilNext(schedule: Schedule): F[Unit] =
    temporal.flatMap(fromNowUntilNext(schedule))(temporal.sleep)

  final def sleep(schedule: Schedule): Stream[F, Unit] =
    Stream.eval(sleepUntilNext(schedule))

  final def awakeEvery(schedule: Schedule): Stream[F, Unit] =
    sleep(schedule).repeat

  final def schedule[A](tasks: List[(Schedule, Stream[F, A])]): Stream[F, A] = {
    val scheduled = tasks.map { case (schedule, task) => awakeEvery(schedule) >> task }
    Stream.emits(scheduled).covary[F].parJoinUnbounded(temporal)
  }
}
