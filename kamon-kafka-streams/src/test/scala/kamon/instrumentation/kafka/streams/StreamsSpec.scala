package kamon.instrumentation.kafka.streams

import java.lang
import java.util.Properties

import kamon.Kamon
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{StreamsConfig, TopologyTestDriver}
import org.apache.kafka.streams.kstream.{Consumed, TimeWindows, Windowed, WindowedSerdes}
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{Grouped, Materialized, Produced}
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.scalatest.WordSpec

import scala.util.Random
import scala.concurrent.duration._
import scala.collection.JavaConverters._

class StreamsSpec extends WordSpec {


  def testProps = {
    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, s"test-${Random.nextLong()}")
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, new lang.Long(10))
    props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG")
    props
  }

  val factory = new ConsumerRecordFactory[String, String](
    Serdes.String().serializer(),
    Serdes.String().serializer()
  )

  implicit val consumed = Consumed.`with`(Serdes.String(), Serdes.String())
  implicit val grouped = Grouped.`with`(Serdes.String(), Serdes.String())
  implicit val mater: Materialized[String, String, ByteArrayWindowStore] =
    Materialized.`with`(Serdes.String(), Serdes.String())
    .withCachingEnabled().withLoggingEnabled(Map.empty[String, String].asJava)
  implicit val produced = Produced.`with`(WindowedSerdes.timeWindowedSerdeFrom(classOf[String]), Serdes.String())


  "dasda" should {
    "dasd dasd" in {
  Kamon.init()
      val builder = new StreamsBuilder()

        builder.stream[String, String]("input")
        .mapValues(a => s"$a-sufix")
        .groupByKey
        .windowedBy(TimeWindows.of(5.seconds.toMillis))
        .reduce((a,b) => a)(mater)
        .toStream
        .through("midtopic")
        .to("end")


      val topo = new TopologyTestDriver(builder.build(), testProps)
      for (i <- 1 to 10000) {
        topo.pipeInput(
          factory.create("input", s"${i % 10}",s"value-${i}")
        )
        Thread.sleep(100)
        topo.readOutput("end")

      }

      1 === 1
    }
  }
}
