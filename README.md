# fs2-cron
[![Build Status](https://travis-ci.org/fthomas/fs2-cron.svg?branch=master)](https://travis-ci.org/fthomas/fs2-cron)
[![codecov](https://codecov.io/gh/fthomas/fs2-cron/branch/master/graph/badge.svg)](https://codecov.io/gh/fthomas/fs2-cron)
[![Scaladex](https://index.scala-lang.org/fthomas/fs2-cron/latest.svg?color=blue)](https://index.scala-lang.org/fthomas/fs2-cron/fs2-cron-core)
[![Scaladoc](https://www.javadoc.io/badge/eu.timepit/fs2-cron-core_2.12.svg?color=blue&label=Scaladoc)](https://javadoc.io/doc/eu.timepit/fs2-cron-core_2.12)

## Quick example

```scala
import cats.effect.IO
import cron4s.Cron
import eu.timepit.fs2cron._
import fs2.{Scheduler, Stream}
import java.time.LocalTime
import scala.concurrent.ExecutionContext.Implicits._
```
```scala
val evenSeconds = Cron.unsafeParse("*/2 * * ? * *")
// evenSeconds: cron4s.expr.CronExpr = */2 * * ? * *

val stream = Scheduler[IO](1).
  flatMap(_.awakeEveryCron[IO](evenSeconds)).
  flatMap(_ => Stream.eval(IO(println(LocalTime.now))))
// stream: fs2.Stream[cats.effect.IO,Unit] = Stream(..)

stream.take(3).compile.drain.unsafeRunSync
// 21:45:28.103
// 21:45:30.004
// 21:45:32.004
```

## Using fs2-cron

The latest version of the library is available for Scala 2.12.

If you're using sbt, add the following to your build:
```sbt
libraryDependencies ++= Seq(
  "eu.timepit" %% "fs2-cron-core" % "<latestVersion>"
)
```

## License

**fs2-cron** is licensed under the Apache License, Version 2.0, available at
http://www.apache.org/licenses/LICENSE-2.0 and also in the
[LICENSE](https://github.com/fthomas/status-page/blob/master/LICENSE) file.
