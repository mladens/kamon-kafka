/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */
package kamon.kafka.streams.instrumentation.advisor

import kamon.kafka.client.instrumentation.HasSpan
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.apache.kafka.clients.consumer.ConsumerRecord

class RecordDeserializerAdvisor
object RecordDeserializerAdvisor {
  @Advice.OnMethodEnter
  def onEnter(@Advice.Argument(1) rawRecord: ConsumerRecord[_, _] with HasSpan): Option[Span] = {
    rawRecord.maybeSpan
  }

  @Advice.OnMethodExit
  def onExit(@Advice.Return deserializedRecord: ConsumerRecord[_, _] with HasSpan, @Advice.Enter span: Option[Span]): Unit = {
    span.foreach { s =>
      deserializedRecord.setSpan(s)
    }
  }
}
