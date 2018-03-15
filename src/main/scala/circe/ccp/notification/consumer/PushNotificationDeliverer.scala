package circe.ccp.notification.consumer

import circe.ccp.notification.domain.{Notification, NotificationType, PushNotificationInfo}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config

import scalaj.http.Http

/**
 * Created by phg on 3/15/18.
 **/
case class PushNotificationDeliverer @Inject()(
  @Named("push-notification-consumer-config") config: Config,
  @Named("one-signal-url") oneSignalUrl: String,
  @Named("one-signal-id") oneSignalId: String,
  @Named("one-signal-key") oneSignalKey: String,
  @Named("push-consume-topic") topics: Seq[String]
) extends NotificationConsumer(config, topics) {

  override def handleCreate(notification: Notification): Unit = {
    val info = notification.data.asJsonObject[PushNotificationInfo]
    notification.notifyType match {
      case NotificationType.PUSH_IOS => pushIOS(notification.receiver, info.message)
      case NotificationType.PUSH_ANDROID => pushAndroid(notification.receiver, info.message)
    }
  }

  private def pushIOS(iosToken: String, message: String): Unit = {
    send(Map(
      "app_id" -> oneSignalId,
      "contents" -> Map(
        "en" -> message
      ),
      "include_ios_tokens" -> Array(iosToken)
    ).toJsonString)
  }

  private def pushAndroid(androidRegId: String, message: String): Unit = {
    send(Map(
      "app_id" -> oneSignalId,
      "contents" -> Map(
        "en" -> message
      ),
      "include_android_reg_ids" -> Array(androidRegId)
    ).toJsonString)
  }

  private def send(body: String): Unit = {
    Http(s"$oneSignalUrl/notifications")
      .header("Content-Type", "application/json; charset=utf-8")
      .header("Authorization", s"Basic $oneSignalKey")
      .postData(body).asString
    info(s"Sent $body")
  }

}
