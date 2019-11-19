package kamon.instrumentation.kafka.streams

import kamon.Kamon
import kamon.tag.TagSet
import org.apache.kafka.common.metrics.Sensor.RecordingLevel
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

import scala.util.control.NonFatal

object MetrisBridge {
  val logger = LoggerFactory.getLogger("kamon.instrumentation.kafka.streams.MetrisBridge")

  case class SensorMetricKey(name: String, recordingLevel: RecordingLevel = RecordingLevel.INFO)

  val StreamsInstrumentationPrefix = "kafka.streams."

  def record(key: SensorMetricKey, metricTags: Map[String, String], value: Double): Unit =
    try {
      Kamon
        .histogram(StreamsInstrumentationPrefix + key.name)
        .withTags(TagSet.from(metricTags))
        .record(value.toLong)
    } catch {
      case NonFatal(t) =>
        logger.warn(s"Failed recording metric ${key.name} due to exception: ${t.getMessage}")
    }

}
