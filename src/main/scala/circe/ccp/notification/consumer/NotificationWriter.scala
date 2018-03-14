package circe.ccp.notification.consumer

import circe.ccp.notification.domain.Notification
import circe.ccp.notification.repository.NotificationRepository
import com.google.inject.Inject
import com.typesafe.config.Config

/**
 * Created by phg on 3/14/18.
 **/
case class NotificationWriter @Inject()(
  consumeConfig: Config,
  notifyRepo: NotificationRepository
) extends NotificationConsumer(consumeConfig) {

  startConsume()

  override def handleCreate(notification: Notification): Unit = {
    notifyRepo.add(notification.id, notification)
  }

}
