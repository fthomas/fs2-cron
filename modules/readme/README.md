# fs2-cron
[![Build Status](https://travis-ci.org/fthomas/fs2-cron.svg?branch=master)](https://travis-ci.org/fthomas/fs2-cron)
[![codecov](https://codecov.io/gh/fthomas/fs2-cron/branch/master/graph/badge.svg)](https://codecov.io/gh/fthomas/fs2-cron)
[![Join the chat at https://gitter.im/fthomas/fs2-cron](https://badges.gitter.im/fthomas/fs2-cron.svg)](https://gitter.im/fthomas/fs2-cron?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://index.scala-lang.org/fthomas/fs2-cron/latest.svg?color=blue)](https://index.scala-lang.org/fthomas/fs2-cron/fs2-cron-core)
[![Scaladoc](https://www.javadoc.io/badge/eu.timepit/fs2-cron-core_2.12.svg?color=blue&label=Scaladoc)](https://javadoc.io/doc/eu.timepit/fs2-cron-core_2.12)

**fs2-cron** is a microlibrary that provides [FS2][FS2] streams based
on [Cron4s][Cron4s] cron expressions.

## Quick examples

```tut:silent
import cats.effect.{IO, Timer}
import cron4s.Cron
import eu.timepit.fs2cron.awakeEveryCron
import fs2.Stream
import java.time.LocalTime
import scala.concurrent.ExecutionContext

implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
```
```tut:book
val evenSeconds = Cron.unsafeParse("*/2 * * ? * *")

val printTime = Stream.eval(IO(println(LocalTime.now)))

val scheduled = awakeEveryCron[IO](evenSeconds) >> printTime

scheduled.take(3).compile.drain.unsafeRunSync
```

```tut:silent
import cats.effect.ContextShift
import eu.timepit.fs2cron.schedule

implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
```
```tut:book
val everyFiveSeconds = Cron.unsafeParse("*/5 * * ? * *")

val scheduledTasks = schedule(List(
  evenSeconds      -> IO(println(LocalTime.now + " task 1")),
  everyFiveSeconds -> IO(println(LocalTime.now + " task 2"))
))

scheduledTasks.take(9).compile.drain.unsafeRunSync
```

## Using fs2-cron

The latest version of the library is available for Scala 2.12.

If you're using sbt, add the following to your build:
```tut:passthrough
{
  val fence = "`" * 3
  print(
    s"""${fence}sbt
       |libraryDependencies ++= Seq(
       |  "eu.timepit" %% "fs2-cron-core" % "${buildinfo.BuildInfo.latestVersion}"
       |)
       |${fence}""".stripMargin.trim
  )
}
```

## License

**fs2-cron** is licensed under the Apache License, Version 2.0, available at
http://www.apache.org/licenses/LICENSE-2.0 and also in the
[LICENSE](https://github.com/fthomas/status-page/blob/master/LICENSE) file.

[Cron4s]: https://github.com/alonsodomin/cron4s
[FS2]: https://github.com/functional-streams-for-scala/fs2
