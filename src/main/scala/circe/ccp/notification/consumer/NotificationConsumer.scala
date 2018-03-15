package circe.ccp.notification.consumer

import circe.ccp.notification.domain.{KafkaCommand, Notification}
import circe.ccp.notification.repository.StringKafkaConsumer
import circe.ccp.notification.util.Jsoning
import com.twitter.util.NonFatal
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerRecord

/**
 * Created by phg on 3/14/18.
 **/
abstract class NotificationConsumer(
  consumerConfig: Config
) extends StringKafkaConsumer(consumerConfig) with Jsoning {

  override def consume(record: ConsumerRecord[String, String]): Unit = {
    KafkaCommand.forName(record.key) match {
      case Some(cmd) => cmd match {
        case KafkaCommand.CREATE => try {
          handleCreate(record.value.asJsonObject[Notification])
        } catch {
          case NonFatal(throwable) => error("NotificationConsumer.consume.CREATE", throwable)
            throw throwable
        }
        case KafkaCommand.UPDATE => try {
          handleUpdate(record.value.asJsonObject[Notification])
        } catch {
          case NonFatal(throwable) => error("NotificationConsumer.consume.UPDATE", throwable)
            throw throwable
        }
        case KafkaCommand.DELETE => try {
          handleDelete(record.value.asJsonObject[Notification].id)
        } catch {
          case NonFatal(throwable) => error("NotificationConsumer.consume.UPDATE", throwable)
            throw throwable
        }
      }
    }

  }

  protected def handleCreate(notification: Notification): Unit = {}

  protected def handleUpdate(notification: Notification): Unit = {}

  protected def handleDelete(id: String): Unit = {}
}
