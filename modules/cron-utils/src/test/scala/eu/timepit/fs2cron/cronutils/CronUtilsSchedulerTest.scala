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

package eu.timepit.fs2cron.cronutils

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import eu.timepit.fs2cron.ZonedDateTimeSchedulerSuite

class CronUtilsSchedulerTest extends ZonedDateTimeSchedulerSuite[ExecutionTime] {
  private val cronDef = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
  private val parser = new CronParser(cronDef)

  override def schedulerCompanion: CronUtilsScheduler.type = CronUtilsScheduler
  override val everySecond: ExecutionTime = ExecutionTime.forCron(parser.parse("* * * ? * *"))
  override val evenSeconds: ExecutionTime = ExecutionTime.forCron(parser.parse("*/2 * * ? * *"))
}
