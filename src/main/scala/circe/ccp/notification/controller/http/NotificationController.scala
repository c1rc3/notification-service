package circe.ccp.notification.controller.http

import circe.ccp.notification.domain._
import circe.ccp.notification.service.NotificationService
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

/**
 * Created by phg on 3/12/18.
 **/
class NotificationController @Inject()(notificationService: NotificationService) extends Controller {

  post("/notifications") {
    req: AddNotificationRequest =>
      NotificationType.forName(req.notifyType) match {
        case Some(notifyType) => notificationService.addNotification(
          req.sender,
          req.receiver,
          notifyType,
          req.data
        ).map(SuccessResponse)
        case _ => response.ok(FailureResponse(FailureReason.InvalidNotifyType))
      }
  }

  get("/notifications/:id") {
    req: Request =>
      notificationService.getNotification(req.params("id")).map({
        case Some(notification) => SuccessResponse(notification)
        case _ => FailureResponse(FailureReason.NotFound)
      })
  }

  get("/notifications/:receiver/receiver") {
    req: GetNotificationRequest => {
      notificationService.getNotifications(
        req.receiver,
        Some(NotificationType.IN_APP),
        req.isRead,
        req.getPageable
      ).map(PagingResponse)
    }
  }

  put("/notifications/:id/read") {
    req: Request => notificationService.markRead(req.params("id")).map(SuccessResponse)
  }

  put("/notifications/:id/unread") {
    req: Request => notificationService.markUnread(req.params("id")).map(SuccessResponse)
  }

  put("/notifications/:receiver/receiver") {
    req: Request => notificationService.markReadAll(req.params("receiver")).map(SuccessResponse)
  }

  get("/notifications/:receiver/num_unread/:notify_type") {
    req: Request => notificationService.getNumUnread(req.params("receiver"), Some(NotificationType.IN_APP))
  }
}
