package circe.ccp.notification.consumer

import circe.ccp.notification.domain.Notification
import circe.ccp.notification.repository.NotificationRepository
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config

/**
 * Created by phg on 3/14/18.
 **/
case class NotificationWriter @Inject()(
  @Named("notification-writer-config") consumeConfig: Config,
  notifyRepo: NotificationRepository,
  @Named("writer-consume-topic") topics: Seq[String]
) extends NotificationConsumer(consumeConfig, topics) {

  override def handleCreate(notification: Notification): Unit = {
    notifyRepo.add(notification.id, notification)
  }

}
