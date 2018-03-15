package circe.ccp.notification.repository

import cakesolutions.kafka.{KafkaConsumer, KafkaProducer}
import circe.ccp.notification.util.Cronning
import circe.ccp.notification.util.TwitterConverters._
import com.twitter.util.{Future, NonFatal}
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.{AuthorizationException, InvalidOffsetException}
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}

import scala.collection.JavaConversions._
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

trait KafkaConsumer[K, V] extends Cronning {
  protected def keyDeserializer: Deserializer[K]

  protected def valueDeserializer: Deserializer[V]

  protected def consumerConfig: Config

  protected def retryWhenFail: Boolean = true

  protected def pollTimeout: Long = 1000L


  private val consumer = KafkaConsumer[K, V](KafkaConsumer.Conf(consumerConfig, keyDeserializer, valueDeserializer))
  consumer.subscribe(consumerConfig.getStringList("topics"))

  private var isRunning = false
  private def _start(): Unit = this.synchronized {
    if (isRunning) return
    isRunning = true
    run(0) {
      var offsets: Map[TopicPartition, OffsetAndMetadata] = Map()
      try {
        val records = consumer.poll(pollTimeout)
        offsets = Map[TopicPartition, OffsetAndMetadata]()
        for (record: ConsumerRecord[K, V] <- records) {
          try {
            consume(record)
            offsets = offsets + (new TopicPartition(record.topic(), record.partition()) -> new OffsetAndMetadata(record.offset()))
            info(s"Consumed ${record.topic()} - ${record.key()} - ${record.offset()}")
          } catch {
            case NonFatal(throwable) => if (retryWhenFail) throw throwable
          }
        }
      } catch {
        case ex: AuthorizationException | InvalidOffsetException => error("KafkaConsumer.poll", ex)
          Thread.currentThread().interrupt()

        case NonFatal(throwable) => error("KafkaConsumer.poll", throwable) // ignore others exception when poll & retry
          Thread.sleep(1000)
      } finally {
        if (offsets.nonEmpty) consumer.commitSync(offsets)
      }
    }
  }

  def startConsume() = _start()

  def consume(record: ConsumerRecord[K, V]): Unit
}

abstract class StringKafkaConsumer(config: Config) extends KafkaConsumer[String, String] {

  override def consumerConfig: Config = config

  override protected def keyDeserializer: Deserializer[String] = new StringDeserializer()

  override protected def valueDeserializer: Deserializer[String] = new StringDeserializer()
}