package kamon.instrumentation.kafka.streams.advisor.metrics

import kamon.instrumentation.kafka.streams.MetrisBridge
import kamon.instrumentation.kafka.streams.MetrisBridge.SensorMetricKey
import kamon.instrumentation.kafka.streams.advisor.metrics.SensorAdvisors.HasSensorMetric
import kanela.agent.api.instrumentation.mixin.Initializer
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.metrics.Sensor.RecordingLevel
import org.apache.kafka.common.metrics.{KafkaMetric, MetricConfig, Sensor}

import scala.collection.JavaConverters._

object SensorAdvisors {

  trait HasSensorMetric {
    def getMetric: SensorMetricKey

    def setMetric(metric: SensorMetricKey)
  }

  class SensorMixin extends HasSensorMetric {
    private var _metric: SensorMetricKey = _

    override def getMetric: SensorMetricKey = _metric

    override def setMetric(metric: SensorMetricKey): Unit = _metric = metric

    @Initializer
    def initialize(): Unit = {
      println("INITIALIZED")
      _metric = null
    }
  }

}


class SensorRecordAdvice
object SensorRecordAdvice {
  @Advice.OnMethodEnter
  def onSensorRecord(
                      @Advice.Argument(0) value: Double,
                      @Advice.This sensor: Sensor with HasSensorMetric,
                      @Advice.FieldValue("recordingLevel") sensorRecordingLevel: RecordingLevel,
                      @Advice.FieldValue("config") metricConfig: MetricConfig,
                      @Advice.FieldValue("metrics") metrics: java.util.Map[MetricName, KafkaMetric]
                    ): Unit =
  if(sensorRecordingLevel.shouldRecord(metricConfig.recordLevel().id)) {
    val metricTags = metrics.asScala.keySet.toSeq.map(_.tags().asScala).flatten.toMap
    MetrisBridge.record(sensor.getMetric, metricTags, value)
  }

}


class CreateThreadLevelSensorAdvice

object CreateThreadLevelSensorAdvice {
  @Advice.OnMethodEnter
  def onCreateSensor(
                      @Advice.Argument(0) sensorName: String,
                      @Advice.Argument(1) recordingLevel: RecordingLevel,
                    ): SensorMetricKey = {
    SensorMetricKey("thread." + sensorName, recordingLevel)
  }

  @Advice.OnMethodExit
  def onExit(@Advice.Enter sensorName: SensorMetricKey, @Advice.Return sensor: Sensor with HasSensorMetric): Unit = {
    sensor.setMetric(sensorName)
  }
}

class CreateNodeLevelSensorAdvice

object CreateNodeLevelSensorAdvice {
  @Advice.OnMethodEnter
  def onCreateSensor(
                      @Advice.Argument(0) taskName: String,
                      @Advice.Argument(1) processorNodeName: String,
                      @Advice.Argument(2) sensorName: String,
                      @Advice.Argument(3) recordingLevel: RecordingLevel
                    ): SensorMetricKey = {
    SensorMetricKey("node." + sensorName, recordingLevel)
  }

  @Advice.OnMethodExit
  def onExit(@Advice.Enter sensorName: SensorMetricKey, @Advice.Return sensor: Sensor with HasSensorMetric): Unit = {
    sensor.setMetric(sensorName)
  }
}

class CreateCacheLevelSensorAdvice

object CreateCacheLevelSensorAdvice {
  @Advice.OnMethodEnter
  def onCreateSensor(
                      @Advice.Argument(0) taskName: String,
                      @Advice.Argument(1) cacheName: String,
                      @Advice.Argument(2) sensorName: String,
                      @Advice.Argument(3) recordingLevel: RecordingLevel
                    ): SensorMetricKey = {
    SensorMetricKey("cache." + sensorName, recordingLevel)
  }

  @Advice.OnMethodExit
  def onExit(@Advice.Enter sensorName: SensorMetricKey, @Advice.Return sensor: Sensor with HasSensorMetric): Unit = {
    sensor.setMetric(sensorName)
  }
}

class CreateStoreLevelSensorAdvice

object CreateStoreLevelSensorAdvice {
  @Advice.OnMethodEnter
  def onCreateSensor(
                      @Advice.Argument(0) taskName: String,
                      @Advice.Argument(1) storeName: String,
                      @Advice.Argument(2) sensorName: String,
                      @Advice.Argument(3) recordingLevel: RecordingLevel
                    ): SensorMetricKey = {
    SensorMetricKey("store." + sensorName, recordingLevel)
  }

  @Advice.OnMethodExit
  def onExit(@Advice.Enter sensorName: SensorMetricKey, @Advice.Return sensor: Sensor with HasSensorMetric): Unit = {
    sensor.setMetric(sensorName)
  }
}


class CreateTaskLevelSensorAdvice

object CreateTaskLevelSensorAdvice {
  @Advice.OnMethodEnter
  def onCreateSensor(
                      @Advice.Argument(0) taskName: String,
                      @Advice.Argument(1) sensorName: String,
                      @Advice.Argument(2) recordingLevel: RecordingLevel,
                    ): SensorMetricKey = {
    SensorMetricKey("task." + sensorName, recordingLevel)
  }

  @Advice.OnMethodExit
  def onExit(@Advice.Enter sensorName: SensorMetricKey, @Advice.Return sensor: Sensor with HasSensorMetric): Unit = {
    sensor.setMetric(sensorName)
  }
}