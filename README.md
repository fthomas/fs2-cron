# fs2-cron
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/fthomas/fs2-cron/Continuous%20Integration)](https://github.com/fthomas/fs2-cron/actions?query=workflow%3A%22Continuous+Integration%22)
[![codecov](https://codecov.io/gh/fthomas/fs2-cron/branch/master/graph/badge.svg)](https://codecov.io/gh/fthomas/fs2-cron)
[![Join the chat at https://gitter.im/fthomas/fs2-cron](https://badges.gitter.im/fthomas/fs2-cron.svg)](https://gitter.im/fthomas/fs2-cron?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://index.scala-lang.org/fthomas/fs2-cron/latest.svg?color=blue)](https://index.scala-lang.org/fthomas/fs2-cron/fs2-cron-core)
[![Scaladoc](https://www.javadoc.io/badge/eu.timepit/fs2-cron-core_2.12.svg?color=blue&label=Scaladoc)](https://javadoc.io/doc/eu.timepit/fs2-cron-core_2.12)

**fs2-cron** is a microlibrary that provides [FS2][FS2] streams based
on [Cron4s][Cron4s] cron expressions.

## Examples

```scala
import cats.effect.{IO, Timer}
import cron4s.Cron
import eu.timepit.fs2cron.awakeEveryCron
import fs2.Stream
import java.time.LocalTime
import scala.concurrent.ExecutionContext

implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
```
```scala
val evenSeconds = Cron.unsafeParse("*/2 * * ? * *")
// evenSeconds: cron4s.CronExpr = */2 * * ? * *

val printTime = Stream.eval(IO(println(LocalTime.now)))
// printTime: fs2.Stream[cats.effect.IO,Unit] = Stream(..)

val scheduled = awakeEveryCron[IO](evenSeconds) >> printTime
// scheduled: fs2.Stream[[x]cats.effect.IO[x],Unit] = Stream(..)

scheduled.take(3).compile.drain.unsafeRunSync
// 05:44:52.290
// 05:44:54.006
// 05:44:56.005
```

```scala
import cats.effect.ContextShift
import eu.timepit.fs2cron.schedule

implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
```
```scala
val everyFiveSeconds = Cron.unsafeParse("*/5 * * ? * *")
// everyFiveSeconds: cron4s.CronExpr = */5 * * ? * *

val scheduledTasks = schedule(List(
  evenSeconds      -> Stream.eval(IO(println(LocalTime.now.toString + " task 1"))),
  everyFiveSeconds -> Stream.eval(IO(println(LocalTime.now.toString + " task 2")))
))
// scheduledTasks: fs2.Stream[[+A]cats.effect.IO[A],Unit] = Stream(..)

scheduledTasks.take(9).compile.drain.unsafeRunSync
// 05:44:58.003 task 1
// 05:45:00.007 task 1
// 05:45:00.007 task 2
// 05:45:02.006 task 1
// 05:45:04.007 task 1
// 05:45:05.006 task 2
// 05:45:06.007 task 1
// 05:45:08.005 task 1
// 05:45:10.004 task 2
// 05:45:10.004 task 1
```

## Using fs2-cron

The latest version of the library is available for Scala 2.12 and 2.13.

If you're using sbt, add the following to your build:

```sbt
libraryDependencies ++= Seq(
  "eu.timepit" %% "fs2-cron-core" % "0.2.2"
)
```

## License

**fs2-cron** is licensed under the Apache License, Version 2.0, available at
http://www.apache.org/licenses/LICENSE-2.0 and also in the
[LICENSE](https://github.com/fthomas/status-page/blob/master/LICENSE) file.

[Cron4s]: https://github.com/alonsodomin/cron4s
[FS2]: https://github.com/functional-streams-for-scala/fs2
