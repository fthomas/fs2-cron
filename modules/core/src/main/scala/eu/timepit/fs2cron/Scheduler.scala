package eu.timepit.fs2cron

import cats.effect.Timer

import scala.concurrent.duration.FiniteDuration

trait Scheduler[F[_], Schedule] {
  def untilNext(schedule: Schedule): F[FiniteDuration]
  def timer: Timer[F]
}
