package kamon.instrumentation.kafka.streams.advisor.metrics


import kamon.instrumentation.kafka.streams.MetrisBridge.SensorMetricKey
import kamon.instrumentation.kafka.streams.advisor.metrics.SensorAdvisors.HasSensorMetric
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.apache.kafka.common.metrics.{MetricConfig, Sensor}


class SensorCreateAdvisor
object SensorCreateAdvisor {

  @Advice.OnMethodEnter
  def onCreateSensor(
                      @Advice.Argument(0) name: String,
                      @Advice.Argument(1) config: MetricConfig,  //Optional
                      @Advice.Argument(3) level: Sensor.RecordingLevel
                    ): SensorMetricKey = SensorMetricKey(name, level)

  @Advice.OnMethodExit
  def onCreatedSensor(@Advice.Enter key: SensorMetricKey, @Advice.Return sensor: Sensor with HasSensorMetric): Unit =
    sensor.setMetric(key)

}
