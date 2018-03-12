package circe.ccp.notification.domain

/**
 * Created by phg on 3/12/18.
 **/
case class Notification(
  id: String,
  sender: String,
  receiver: String,
  data: String,
  notifyType: String,
  isRead: Boolean,
  createdTime: Long,
  updatedTime: Long,
  readTime: Long
)
