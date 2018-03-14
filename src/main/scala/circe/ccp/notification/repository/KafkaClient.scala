package circe.ccp.notification.repository

import cakesolutions.kafka.{KafkaConsumer, KafkaProducer}
import circe.ccp.notification.util.Cronning
import circe.ccp.notification.util.TwitterConverters._
import com.twitter.util.{Future, NonFatal}
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by phg on 3/13/18.
 **/

trait KafkaProducer[K, V] {
  protected def keySerializer: Serializer[K]

  protected def valueSerializer: Serializer[V]

  protected def producerConfig: Config

  private val producer = KafkaProducer(KafkaProducer.Conf(producerConfig, keySerializer, valueSerializer))

  def send(topic: String, key: K, value: V, partition: java.lang.Integer = null, timestamp: java.lang.Long = null)
    (implicit ec: ExecutionContext = global): Future[RecordMetadata] =
    producer.send(new ProducerRecord[K, V](topic, partition, timestamp, key, value))
}

case class StringKafkaProducer(producerConfig: Config) extends KafkaProducer[String, String] {

  override protected def keySerializer: Serializer[String] = new StringSerializer()

  override protected def valueSerializer: Serializer[String] = new StringSerializer()
}

trait KafkaConsumer[K, V] extends KafkaProducer[K, V] with Cronning {
  protected def keyDeserializer: Deserializer[K]

  protected def valueDeserializer: Deserializer[V]

  protected def consumerConfig: Config

  protected def rescheduleWhenFail: Boolean = false

  protected def retryWhenFail: Boolean = true

  private val consumer = KafkaConsumer[K, V](KafkaConsumer.Conf(consumerConfig, keyDeserializer, valueDeserializer))

  // no-delay
  run(0) {
    try {
      val records = consumer.poll(1000)
      for (record: ConsumerRecord[K, V] <- records) {
        try {
          consume(record)
          info(s"Consumed ${record.topic()} - ${record.key()} - ${record.offset()}")
        } catch {
          case NonFatal(throwable) => if (rescheduleWhenFail) {
            reschedule(record)
          } else if (retryWhenFail) {
            throw throwable
          }
        }
      }
      consumer.commitSync()
    } catch {
      case NonFatal(throwable) => error("KafkaConsumer.poll", throwable) // ignore all exception when poll & retry
    }
  }

  def consume(record: ConsumerRecord[K, V]): Unit

  def reschedule(record: ConsumerRecord[K, V]) = send(record.topic(), record.key(), record.value(), record.partition())
}

abstract class StringKafkaConsumer(producerConfig: Config, consumerConfig: ConsumerConfig) extends KafkaConsumer[String, String] {
  override protected def keyDeserializer: Deserializer[String] = new StringDeserializer()

  override protected def valueDeserializer: Deserializer[String] = new StringDeserializer()

  override protected def keySerializer: Serializer[String] = new StringSerializer()

  override protected def valueSerializer: Serializer[String] = new StringSerializer()
}