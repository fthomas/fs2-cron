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
// cronScheduler: eu.timepit.fs2cron.Scheduler[IO, cron4s.expr.CronExpr] = eu.timepit.fs2cron.cron4s.Cron4sScheduler$$anon$1@3b612fa5

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
// 22:34:46.145109392
// 22:34:48.001058522
// 22:34:50.001971813
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
// 22:34:52.001721651 task 1
// 22:34:54.001702294 task 1
// 22:34:55.002032079 task 2
// 22:34:56.000919733 task 1
// 22:34:58.000939089 task 1
// 22:35:00.001158852 task 1
// 22:35:00.001743567 task 2
// 22:35:02.001037743 task 1
// 22:35:04.001006980 task 1
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
// calevScheduler: eu.timepit.fs2cron.Scheduler[IO, CalEvent] = eu.timepit.fs2cron.calev.CalevScheduler$$anon$1@5b72ab85
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
// 22:35:05.005121709
// 22:35:07.000442205
// 22:35:09.000989961
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
// 22:35:11.000475346 task 1
// 22:35:12.000865309 task 2
// 22:35:13.000723319 task 1
// 22:35:15.001042916 task 1
// 22:35:16.000423061 task 2
// 22:35:17.000806045 task 1
// 22:35:19.000158236 task 1
// 22:35:20.000879932 task 2
// 22:35:21.000786710 task 1
```

## Using fs2-cron

The latest version of the library is available for Scala 2.12 and 2.13.

If you're using sbt, add the following to your build:
```sbt
libraryDependencies ++= Seq(
  "eu.timepit" %% "fs2-cron-cron4s" % "0.9.0" //and/or
  "eu.timepit" %% "fs2-cron-calev" % "0.9.0"
)
```

## License

**fs2-cron** is licensed under the Apache License, Version 2.0, available at
http://www.apache.org/licenses/LICENSE-2.0 and also in the
[LICENSE](https://github.com/fthomas/status-page/blob/master/LICENSE) file.

[Cron4s]: https://github.com/alonsodomin/cron4s
[FS2]: https://github.com/functional-streams-for-scala/fs2
[Calev]: https://github.com/eikek/calev
