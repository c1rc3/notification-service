package circe.ccp.notification.service

import circe.ccp.notification.domain.{Notification, Page, Pageable}
import com.twitter.util.Future

/**
 * Created by plt on 12/26/17.
 **/
trait NotificationService {
  def addNotification(sender: String, receiver: String, notifyType: String, data: String): Future[String]

  def getNotification(id: String): Future[Option[Notification]]

  def markRead(notificationId: String): Future[Boolean]

  def markReadAll(receiver: String): Future[Int]

  def markUnread(receiver: String, notificationId: String): Future[Boolean]

  def getNotifications(
    receiver: String,
    isRead: Option[Boolean] = None,
    notifyType: Option[String] = None,
    pageable: Pageable,
    sorts: Seq[String]
  ): Future[Page[Notification]]

  def getNumUnread(receiver: String, notifyType: Option[String] = None): Future[Long]
}

