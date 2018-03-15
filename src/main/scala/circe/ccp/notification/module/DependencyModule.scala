package circe.ccp.notification.module

import javax.inject.Singleton

import circe.ccp.notification.repository.{ESNotificationRepository, ElasticsearchRepository, NotificationRepository}
import circe.ccp.notification.service.{FakedNotificationService, NotificationService}
import circe.ccp.notification.util.ZConfig
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import com.typesafe.config.Config

import scala.collection.JavaConversions._
/**
 * Created by phg on 3/12/18.
 **/
object DependencyModule extends TwitterModule {
  override def configure(): Unit = {
    super.configure()

    bind[String].annotatedWithName("notification-type-name").toInstance("")
    bind[String].annotatedWithName("notification-topic-prefix").toInstance("")
    bind[String].annotatedWithName("notification-mapping-file").toInstance("")
    bind[String].annotatedWithName("one-signal-id").toInstance("")
    bind[String].annotatedWithName("one-signal-key").toInstance("")


    bind[Config].annotatedWithName("email-notification-consumer-config").toInstance(ZConfig.getConf(""))
    bind[Config].annotatedWithName("notification-writer-config").toInstance(ZConfig.getConf(""))
    bind[Config].annotatedWithName("smtp-config").toInstance(ZConfig.getConf(""))

    bind[NotificationRepository].to[ESNotificationRepository]
    bind[NotificationService].to[FakedNotificationService]
  }

  @Provides
  @Singleton
  def providesESClient: ElasticsearchRepository = {
    val config = ZConfig.getConf("notification")
    ElasticsearchRepository(
      config.getStringList("es.servers").toList,
      config.getString("es.cluster"),
      config.getBoolean("es.transport-sniff"),
      config.getString("es.index-name")
    )
  }

}
