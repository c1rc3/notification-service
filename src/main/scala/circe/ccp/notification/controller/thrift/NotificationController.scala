package circe.ccp.notification.controller.thrift

import javax.inject.Inject

import circe.ccp.notification.domain.{Notification, NotificationType, Page, PageNumberRequest}
import circe.ccp.notification.service.NotificationService
import circe.cpp.notification.TNotificationService._
import circe.cpp.notification._
import com.twitter.finatra.thrift.Controller

/**
 * Created by phg on 3/12/18.
 **/
class NotificationController @Inject()(notifyService: NotificationService) extends Controller with TNotificationService.BaseServiceIface {
  override def addNotification = handle(AddNotification) {
    args: AddNotification.Args => {
      notifyService.addNotification(
        sender = args.sender,
        receiver = args.receiver,
        notifyType = NotificationType.forName(args.notifyType).get,
        data = args.data
      )
    }
  }

  override def markRead = handle(MarkRead) {
    args: MarkRead.Args => {
      notifyService.markRead(args.notificationId)
    }
  }

  override def markUnread = handle(MarkUnread) {
    args: MarkUnread.Args => {
      notifyService.markUnread(args.receiver, args.notificationId)
    }
  }

  override def markReadAll = handle(MarkReadAll) {
    args: MarkReadAll.Args => {
      notifyService.markReadAll(args.receiver)
    }
  }

  override def getNotifications = handle(GetNotifications) {
    args: GetNotifications.Args => {
      notifyService.getNotifications(
        receiver = args.receiver,
        notifyType = args.notifyType.flatMap(s => NotificationType.forName(s)),
        pageable = PageNumberRequest(args.page, args.size),
        sorts = args.sorts.getOrElse(Seq[String]())
      ).map(Page2Thrift)
    }
  }

  override def getUnRead = handle(GetUnRead) {
    args: GetUnRead.Args => {
      notifyService.getNotifications(
        receiver = args.receiver,
        notifyType = args.notifyType.flatMap(s => NotificationType.forName(s)),
        isRead = Some(false),
        pageable = PageNumberRequest(args.page, args.size),
        sorts = args.sorts.getOrElse(Seq[String]())
      ).map(Page2Thrift)
    }
  }

  override def numUnread = handle(NumUnread) {
    args: NumUnread.Args => {
      notifyService.getNumUnread(args.receiver, args.notifyType.flatMap(s => NotificationType.forName(s)))
    }
  }

  private def Page2Thrift(page: Page[Notification]): TListNotificationResponse = TListNotificationResponse(
    contents = page.content.map(Notification2Thrift),
    totalElement = page.totalElement,
    totalPage = page.totalPage,
    currentPage = page.currentPage
  )

  private def Notification2Thrift(n: Notification): TNotification = TNotification(
    id = n.id,
    sender = n.sender,
    receiver = n.receiver,
    notifyType = n.notifyType.toString,
    data = n.data,
    isRead = n.isRead,
    createdTime = n.createdTime,
    updatedTime = n.updatedTime,
    readTime = n.readTime
  )
}