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

package eu.timepit.fs2cron.cron4s

import cron4s.Cron
import cron4s.expr.CronExpr
import eu.timepit.fs2cron.ZonedDateTimeSchedulerSuite

class Cron4sSchedulerTest extends ZonedDateTimeSchedulerSuite[CronExpr] {
  override def schedulerCompanion: Cron4sScheduler.type = Cron4sScheduler
  override val everySecond: CronExpr = Cron.unsafeParse("* * * ? * *")
  override val evenSeconds: CronExpr = Cron.unsafeParse("*/2 * * ? * *")
}
