package circe.ccp.notification.service

import circe.ccp.notification.domain.NotificationType.NotificationType
import circe.ccp.notification.domain._
import circe.ccp.notification.repository.{NotificationRepository, StringKafkaProducer}
import circe.ccp.notification.util.{Jsoning, StringUtil}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.twitter.util.Future

/**
 * Created by plt on 12/26/17.
 **/
trait NotificationService {
  def addNotification(sender: String, receiver: String, notifyType: NotificationType, data: String): Future[String]

  def getNotification(id: String): Future[Option[Notification]]

  def markRead(notificationId: String): Future[Boolean]

  def markReadAll(receiver: String): Future[Int]

  def markUnread(notificationId: String): Future[Boolean]

  def getNotifications(
    receiver: String,
    notifyType: Option[NotificationType] = None,
    isRead: Option[Boolean] = None,
    pageable: Pageable
  ): Future[Page[Notification]]

  def getNumUnread(receiver: String, notifyType: Option[NotificationType] = None): Future[Long]
}

case class FakedNotificationService() extends NotificationService {
  override def addNotification(sender: String, receiver: String, notifyType: NotificationType, data: String) = Future("abc-xyz")

  override def getNotification(id: String) = Future(Some(Notification(
    id = "abc-xyz",
    sender = "sender-1",
    receiver = "receiver-1",
    notifyType = NotificationType.IN_APP,
    isRead = false,
    data = "{}",
    createdTime = 1520838362136L,
    updatedTime = 1520838362136L,
    readTime = 0L
  )))

  override def markRead(notificationId: String) = Future(true)

  override def markReadAll(receiver: String) = Future(10)

  override def markUnread(notificationId: String) = Future(true)

  override def getNotifications(receiver: String, notifyType: Option[NotificationType] = None, isRead: Option[Boolean], pageable: Pageable) = Future {
    PageImpl(
      content = Array(Notification(
        id = "abc-xyz",
        sender = "sender-1",
        receiver = "receiver-1",
        notifyType = notifyType.getOrElse(NotificationType.IN_APP),
        isRead = false,
        data = "{}",
        createdTime = 1520838362136L,
        updatedTime = 1520838362136L,
        readTime = 0L
      )), pageable, total = 100
    )
  }

  override def getNumUnread(receiver: String, notifyType: Option[NotificationType] = None) = Future(10)
}

case class KafkaNotificationService @Inject()(
  kafkaProducer: StringKafkaProducer,
  @Named("notification-topic-prefix") topicPrefix: String,
  notifyRepo: NotificationRepository
) extends NotificationService with Jsoning {

  override def addNotification(sender: String, receiver: String, notifyType: NotificationType, data: String) = {
    val id = StringUtil.genUniqueId
    val currentMillis = System.currentTimeMillis()
    kafkaProducer.send(s"$topicPrefix-${notifyType.toString.toLowerCase}", KafkaCommand.CREATE.toString, Notification(
      id = id,
      sender = sender,
      receiver = receiver,
      data = data,
      notifyType = notifyType,
      isRead = false,
      createdTime = currentMillis,
      updatedTime = currentMillis,
      readTime = 0L,
    ).toJsonString).map(_ => id)
  }

  override def getNotification(id: String) = notifyRepo.get(id)

  override def markRead(notifyId: String) = notifyRepo.update(notifyId, Map("is_read" -> true).toJsonString)

  override def markReadAll(receiver: String) = {
    notifyRepo.search(receiver = Some(receiver), isRead = Some(false), pageable = PageNumberRequest(1, 1000)).map(page => {
      page.content.foreach(n => markRead(n.id))
      page.content.length
    })
  }

  override def markUnread(notifyId: String) = notifyRepo.update(notifyId, Map("is_read" -> false).toJsonString)

  override def getNotifications(receiver: String, notifyType: Option[NotificationType], isRead: Option[Boolean], pageable: Pageable) = {
    notifyRepo.search(
      receiver = Some(receiver),
      notifyType = notifyType,
      isRead = isRead,
      pageable = pageable
    )
  }

  override def getNumUnread(receiver: String, notifyType: Option[NotificationType]) = {
    notifyRepo.count(
      receiver = Some(receiver),
      notifyType = notifyType
    )
  }
}