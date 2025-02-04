# fs2-cron
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/fthomas/fs2-cron/ci.yml?branch=master)](https://github.com/fthomas/fs2-cron/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/fthomas/fs2-cron/branch/master/graph/badge.svg)](https://codecov.io/gh/fthomas/fs2-cron)
[![Join the chat at https://gitter.im/fthomas/fs2-cron](https://badges.gitter.im/fthomas/fs2-cron.svg)](https://gitter.im/fthomas/fs2-cron?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://index.scala-lang.org/fthomas/fs2-cron/latest.svg?color=blue)](https://index.scala-lang.org/fthomas/fs2-cron/fs2-cron-core)
[![Scaladoc](https://www.javadoc.io/badge/eu.timepit/fs2-cron-core_2.12.svg?color=blue&label=Scaladoc)](https://javadoc.io/doc/eu.timepit/fs2-cron-core_2.12)

**fs2-cron** is a microlibrary that provides [FS2][FS2] streams based
on [Cron4s][Cron4s] cron expressions or [Calev][Calev] calendar
events.

It is provided for Scala 2.12, 2.13 and `fs2-cron-calev` also for
Scala 3.

## Examples

```scala
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream
import java.time.LocalTime

val printTime = Stream.eval(IO(println(LocalTime.now)))
```

### Using Cron4s library

Requires the `fs2-cron-cron4s` module:

```scala
import cron4s.Cron
import eu.timepit.fs2cron.cron4s.Cron4sScheduler

val cronScheduler = Cron4sScheduler.systemDefault[IO]
// cronScheduler: eu.timepit.fs2cron.Scheduler[IO, cron4s.expr.CronExpr] = eu.timepit.fs2cron.cron4s.Cron4sScheduler$$anon$1@625bb073

val evenSeconds = Cron.unsafeParse("*/2 * * ? * *")
// evenSeconds: cron4s.package.CronExpr = CronExpr(
//   seconds = */2,
//   minutes = *,
//   hours = *,
//   daysOfMonth = ?,
//   months = *,
//   daysOfWeek = *
// )

val scheduled = cronScheduler.awakeEvery(evenSeconds) >> printTime
// scheduled: Stream[[x]IO[x], Unit] = Stream(..)

scheduled.take(3).compile.drain.unsafeRunSync()
// 12:38:40.031512447
// 12:38:42.001027177
// 12:38:44.000518647
```
```scala
val everyFiveSeconds = Cron.unsafeParse("*/5 * * ? * *")
// everyFiveSeconds: cron4s.package.CronExpr = CronExpr(
//   seconds = */5,
//   minutes = *,
//   hours = *,
//   daysOfMonth = ?,
//   months = *,
//   daysOfWeek = *
// )

val scheduledTasks = cronScheduler.schedule(List(
  evenSeconds      -> Stream.eval(IO(println(LocalTime.now.toString + " task 1"))),
  everyFiveSeconds -> Stream.eval(IO(println(LocalTime.now.toString + " task 2")))
))
// scheduledTasks: Stream[IO, Unit] = Stream(..)

scheduledTasks.take(9).compile.drain.unsafeRunSync()
// 12:38:45.001594555 task 2
// 12:38:46.000972112 task 1
// 12:38:48.001605611 task 1
// 12:38:50.000343600 task 2
// 12:38:50.000664246 task 1
// 12:38:52.000439584 task 1
// 12:38:54.000618682 task 1
// 12:38:55.000250687 task 2
// 12:38:56.000902626 task 1
```

#### Cancelling the scheduled task
Using `Stream#interruptWhen(haltWhenTrue)`

```scala
import cats.effect._
import cron4s.Cron
import eu.timepit.fs2cron.cron4s.Cron4sScheduler
import fs2.Stream
import fs2.concurrent.SignallingRef

import java.time.LocalTime
import scala.concurrent.duration._

object TestApp extends IOApp.Simple {
  val printTime = Stream.eval(IO(println(LocalTime.now)))

  override def run: IO[Unit] = {
    val cronScheduler = Cron4sScheduler.systemDefault[IO]
    val evenSeconds = Cron.unsafeParse("*/2 * * ? * *")
    val scheduled = cronScheduler.awakeEvery(evenSeconds) >> printTime
    val cancel = SignallingRef[IO, Boolean](false)

    for {
      c <- cancel
      s <- scheduled.interruptWhen(c).repeat.compile.drain.start
      //prints about 5 times before stop
      _ <- Temporal[IO].sleep(10.seconds) >> c.set(true)
    } yield s
  }
}
```

### Using Calev library

Requires the `fs2-cron-calev` module:

```scala
import com.github.eikek.calev.CalEvent
import eu.timepit.fs2cron.calev.CalevScheduler

val calevScheduler = CalevScheduler.systemDefault[IO]
// calevScheduler: eu.timepit.fs2cron.Scheduler[IO, CalEvent] = eu.timepit.fs2cron.calev.CalevScheduler$$anon$1@6ed71de5
val oddSeconds = CalEvent.unsafe("*-*-* *:*:1/2")
// oddSeconds: CalEvent = CalEvent(
//   weekday = All,
//   date = DateEvent(year = All, month = All, day = All),
//   time = TimeEvent(
//     hour = All,
//     minute = All,
//     seconds = List(values = Vector(Single(value = 1, rep = Some(value = 2))))
//   ),
//   zone = None
// )

val calevScheduled = calevScheduler.awakeEvery(oddSeconds) >> printTime
// calevScheduled: Stream[[x]IO[x], Unit] = Stream(..)
calevScheduled.take(3).compile.drain.unsafeRunSync()
// 12:38:57.006764100
// 12:38:59.000609396
// 12:39:01.000623646
```

```scala
val everyFourSeconds = CalEvent.unsafe("*-*-* *:*:0/4")
// everyFourSeconds: CalEvent = CalEvent(
//   weekday = All,
//   date = DateEvent(year = All, month = All, day = All),
//   time = TimeEvent(
//     hour = All,
//     minute = All,
//     seconds = List(values = Vector(Single(value = 0, rep = Some(value = 4))))
//   ),
//   zone = None
// )

val calevScheduledTasks = calevScheduler.schedule(List(
  oddSeconds      -> Stream.eval(IO(println(LocalTime.now.toString + " task 1"))),
  everyFourSeconds -> Stream.eval(IO(println(LocalTime.now.toString + " task 2")))
))
// calevScheduledTasks: Stream[IO, Unit] = Stream(..)

calevScheduledTasks.take(9).compile.drain.unsafeRunSync()
// 12:39:02.999975951 task 1
// 12:39:03.999878983 task 2
// 12:39:05.000458555 task 1
// 12:39:07.000196942 task 1
// 12:39:08.000452908 task 2
// 12:39:08.999946252 task 1
// 12:39:11.000792975 task 1
// 12:39:12.000086810 task 2
// 12:39:13.000453110 task 1
```

## Using fs2-cron

The latest version of the library is available for Scala 2.12 and 2.13.

If you're using sbt, add the following to your build:
```sbt
libraryDependencies ++= Seq(
  "eu.timepit" %% "fs2-cron-cron4s" % "0.10.0" //and/or
  "eu.timepit" %% "fs2-cron-calev" % "0.10.0"
)
```

## License

**fs2-cron** is licensed under the Apache License, Version 2.0, available at
http://www.apache.org/licenses/LICENSE-2.0 and also in the
[LICENSE](https://github.com/fthomas/status-page/blob/master/LICENSE) file.

[Cron4s]: https://github.com/alonsodomin/cron4s
[FS2]: https://github.com/functional-streams-for-scala/fs2
[Calev]: https://github.com/eikek/calev
