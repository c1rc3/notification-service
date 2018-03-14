package circe.ccp.notification.domain

import circe.ccp.notification.domain.NotificationType.NotificationType
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

/**
 * Created by phg on 3/12/18.
 **/
case class Notification(
  id: String,
  sender: String,
  receiver: String,
  data: String,
  @JsonScalaEnumeration(classOf[TNotificationType]) notifyType: NotificationType,
  isRead: Boolean,
  createdTime: Long,
  updatedTime: Long,
  readTime: Long
)

object NotificationType extends Enumeration {
  type NotificationType = Value

  val PUSH_IOS = Value(1, "PUSH_IOS")
  val PUSH_ANDROID = Value(2, "PUSH_ANDROID")
  val EMAIL = Value(3, "EMAIL")
  val SMS = Value(4, "SMS")
  val IN_APP = Value(5, "IN_APP")

  def forName(s: String): Option[Value] = values.find(_.toString == s)
}

class TNotificationType extends TypeReference[NotificationType.type]