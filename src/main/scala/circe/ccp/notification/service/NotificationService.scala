package circe.ccp.notification.service

import circe.ccp.notification.domain.{Notification, Page, PageImpl, Pageable}
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

case class FakedNotificationService() extends NotificationService {
  override def addNotification(sender: String, receiver: String, notifyType: String, data: String) = Future("abc-xyz")

  override def getNotification(id: String) = Future(Some(Notification(
    id = "abc-xyz",
    sender = "sender-1",
    receiver = "receiver-1",
    notifyType = "InApp",
    isRead = false,
    data = "{}",
    createdTime = 1520838362136L,
    updatedTime = 1520838362136L,
    readTime = 0L
  )))

  override def markRead(notificationId: String) = Future(true)

  override def markReadAll(receiver: String) = Future(10)

  override def markUnread(receiver: String, notificationId: String) = Future(true)

  override def getNotifications(receiver: String, isRead: Option[Boolean], notifyType: Option[String], pageable: Pageable, sorts: Seq[String]) = Future {
    PageImpl(
      content = Array(Notification(
        id = "abc-xyz",
        sender = "sender-1",
        receiver = "receiver-1",
        notifyType = "InApp",
        isRead = false,
        data = "{}",
        createdTime = 1520838362136L,
        updatedTime = 1520838362136L,
        readTime = 0L
      )), pageable, total = 100
    )
  }

  override def getNumUnread(receiver: String, notifyType: Option[String]) = Future(10)
}