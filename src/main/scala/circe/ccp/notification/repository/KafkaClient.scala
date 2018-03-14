package circe.ccp.notification.repository

import cakesolutions.kafka.KafkaProducer
import cakesolutions.kafka.KafkaProducer.Conf
import com.twitter.util.Future
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import circe.ccp.notification.util.TwitterConverters._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by phg on 3/13/18.
 **/

trait KafkaProducer[K, V] {
  protected def keySerializer: Serializer[K]

  protected def valueSerializer: Serializer[V]

  protected def config: Config

  private val producer = KafkaProducer(Conf(config, keySerializer, valueSerializer))

  def send(topic: String, key: K, value: V, partition: java.lang.Integer = null, timestamp: java.lang.Long = null)
    (implicit ec: ExecutionContext = global): Future[RecordMetadata] =
  producer.send(new ProducerRecord[K, V](topic, partition, timestamp, key, value))
}

case class StringKafkaProducer(config: Config) extends KafkaProducer[String, String] {

  override protected def keySerializer: Serializer[String] = new StringSerializer()

  override protected def valueSerializer: Serializer[String] = new StringSerializer()
}