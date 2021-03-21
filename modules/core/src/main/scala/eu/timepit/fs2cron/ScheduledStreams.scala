package eu.timepit.fs2cron

import cats.effect.Concurrent
import fs2.Stream

final class ScheduledStreams[F[_], Schedule](scheduler: Scheduler[F, Schedule]) {
  def sleep(schedule: Schedule): Stream[F, Unit] =
    Stream.eval(scheduler.untilNext(schedule)).flatMap(Stream.sleep[F](_)(scheduler.timer))

  def awakeEvery(schedule: Schedule): Stream[F, Unit] =
    sleep(schedule).repeat

  def schedule[A](tasks: List[(Schedule, Stream[F, A])])(implicit
      F: Concurrent[F]
  ): Stream[F, A] = {
    val scheduled = tasks.map { case (schedule, task) => awakeEvery(schedule) >> task }
    Stream.emits(scheduled).covary[F].parJoinUnbounded
  }
}
