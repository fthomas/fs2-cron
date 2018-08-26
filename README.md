# fs2-cron
[![Build Status](https://travis-ci.org/fthomas/fs2-cron.svg?branch=master)](https://travis-ci.org/fthomas/fs2-cron)
[![codecov](https://codecov.io/gh/fthomas/fs2-cron/branch/master/graph/badge.svg)](https://codecov.io/gh/fthomas/fs2-cron)

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
// 16:54:34.102
// 16:54:36.003
// 16:54:38.006
```

## License

**fs2-cron** is licensed under the Apache License, Version 2.0, available at
http://www.apache.org/licenses/LICENSE-2.0 and also in the
[LICENSE](https://github.com/fthomas/status-page/blob/master/LICENSE) file.
