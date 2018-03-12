package circe.ccp.notification.module

import circe.ccp.notification.service.{FakedNotificationService, NotificationService}
import com.twitter.inject.TwitterModule

/**
 * Created by phg on 3/12/18.
 **/
object DependencyModule extends TwitterModule {
  override def configure(): Unit = {
    super.configure()

    bind[NotificationService].to[FakedNotificationService]
  }
}
