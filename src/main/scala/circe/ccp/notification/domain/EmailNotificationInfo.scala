package circe.ccp.notification.domain

/**
 * Created by phg on 3/15/18.
 **/
case class EmailNotificationInfo(
  to: Array[String],
  cc: Array[String],
  bcc: Array[String],
  subject: String,
  body: String
)
