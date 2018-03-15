package circe.ccp.notification.consumer

import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config

/**
 * Created by phg on 3/15/18.
 **/
class PushNotificationDeliverer @Inject()(
  @Named("email-notification-consumer-config") config: Config,
  @Named("one-signal-id") oneSignalId: String,
  @Named("one-signal-key") oneSignalKey: String,
) extends NotificationConsumer(config) {

}
