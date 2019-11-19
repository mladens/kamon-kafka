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
package kamon.instrumentation.kafka.streams

import kamon.instrumentation.context.HasContext
import kamon.instrumentation.kafka.streams.advisor.HasConsumerRecord.Mixin
import kamon.instrumentation.kafka.streams.advisor._
import kamon.instrumentation.kafka.streams.advisor.metrics.SensorAdvisors.SensorMixin
import kamon.instrumentation.kafka.streams.advisor.metrics.{CreateCacheLevelSensorAdvice, CreateNodeLevelSensorAdvice, CreateStoreLevelSensorAdvice, CreateTaskLevelSensorAdvice, CreateThreadLevelSensorAdvice, SensorCreateAdvisor, SensorRecordAdvice}
import kanela.agent.api.instrumentation.InstrumentationBuilder

class StreamsInstrumentation extends InstrumentationBuilder {

  /*Instrument raw sensor factory to collect sensor name and tags.
  * Necessary since not all (but most) sensors are created through `StreamsMetricsImpl.xxxLevelSensor`
  **/
  onType("org.apache.kafka.common.metrics.Metrics")
    .advise(method("sensor").and(takesArguments(5)), classOf[SensorCreateAdvisor]) //applies SensorName, scopedSensors will have this overriden by StreamsMetricsImpl


  /*Wiretap sensor recordings and apply them to Kamon instruments.
  * Additional metrics added to sensor are different measures over same data and can be extracted from Kamon histogram
  * so there's no need for extra instruments here. Metrics might bring additional tags on top of sensor's own ones.
  * */
  onType("org.apache.kafka.common.metrics.Sensor")
    .mixin(classOf[SensorMixin])
    .advise(method("record").and(takesArguments(3)), classOf[SensorRecordAdvice])

  /*Instrument senosor factories to extract Kamon metric name. Sensor scope is decided based on
  * particular method being invoked while rest of context is used to further refine metric
  * This will override any metric name previously set by Metrics instrumentation (hopefully with a more refined one)*/
  onType("org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl")
    .advise(method("threadLevelSensor"), classOf[CreateThreadLevelSensorAdvice])
    .advise(method("nodeLevelSensor"), classOf[CreateNodeLevelSensorAdvice])
    .advise(method("cacheLevelSensor"), classOf[CreateCacheLevelSensorAdvice])
    .advise(method("storeLevelSensor"), classOf[CreateStoreLevelSensorAdvice])
    .advise(method("taskLevelSensor"), classOf[CreateTaskLevelSensorAdvice])




  //TODO mladens this might not be necessary, upcast StampedRecord to Stamped and extract .value()
  /**
    * This is required to access the original ConsumerRecord wrapped by StampedRecord
    * in order to extract his span. This span can then be used a parent for the
    * span representing the stream.
    */
  onSubTypesOf("org.apache.kafka.streams.processor.internals.StampedRecord")
    .mixin(classOf[Mixin])
    .advise(isConstructor, classOf[StampedRecordAdvisor])

  //TODO mladens context is potentially already stored on thread, maybe just instrument ConsumerRecord ctor to just pickup whatever context is available at instantiation point
  /**
    * This propagates the span from the original "raw" record to the "deserialized" record
    * that is processed by the stream.
    */
  onType("org.apache.kafka.streams.processor.internals.RecordDeserializer")
    .advise(method("deserialize"), classOf[RecordDeserializerAdvisor])

  /**
    * Instrument KStream's AbstractProcessor.process for creating node-specifc spans.
    * This can be enabled/disable via configuration.
    * Also provide a bridge to access the internal processor context which carries
    * the Kamon context.
    */
  onSubTypesOf("org.apache.kafka.streams.processor.AbstractProcessor")
    .advise(method("process"), classOf[ProcessorProcessMethodAdvisor])
    .bridge(classOf[ProcessorContextBridge])

  /**
    * Instruments org.apache.kafka.streams.processor.internals.StreamTask::updateProcessorContext
    *
    * UpdateProcessContextAdvisor: this will start the stream span
    * ProcessMethodAdvisor: this will finish the stream span
    *
    */
  onType("org.apache.kafka.streams.processor.internals.StreamTask")
    .advise(method("updateProcessorContext"), classOf[StreamTaskUpdateProcessContextAdvisor])
    .advise(method("process"), classOf[StreamTaskProcessMethodAdvisor])
  //TODO mladens we could place context in ProcessorContext, and have advice around only one method
  //and that is actual node.process that is doing the processing
  //Event heir code is doing measuring that way


  /**
    * Keep the stream's Kamon context on the (Abstract)ProcessorContext so that it is available
    * to all participants of this stream's processing.
    */
  onSubTypesOf("org.apache.kafka.streams.processor.internals.AbstractProcessorContext")
    .mixin(classOf[HasContext.VolatileMixin])

  /**
    * Propagate the Kamon context from ProcessorContext to the RecordCollector.
    */
  onSubTypesOf("org.apache.kafka.streams.processor.internals.RecordCollector$Supplier")
    .advise(method("recordCollector"), classOf[RecordCollectorSupplierAdvisor])

  /**
    * Have the Kamon context available, store it when invoking the send method so that it can be picked
    * up by the ProducerInstrumentation and close it after the call.
    *
    * It must match the correct version of the send method since there are two with different signatures,
    * one delegating to the other. Without the match on the 4th argument both send methods would be instrumented.
    */
  onSubTypesOf("org.apache.kafka.streams.processor.internals.RecordCollector")
    .mixin(classOf[HasContext.VolatileMixin])
    .advise(method("send").and(withArgument(4, classOf[Integer])), classOf[RecordCollectorSendAdvisor])


}
