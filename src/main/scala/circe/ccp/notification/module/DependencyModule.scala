package circe.ccp.notification.module

import javax.inject.Singleton

import circe.ccp.notification.repository.{ESNotificationRepository, ElasticsearchRepository, NotificationRepository, StringKafkaProducer}
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

    bind[String].annotatedWithName("notification-type-name").toInstance(ZConfig.getString("notification.es.type-name"))
    bind[String].annotatedWithName("notification-topic-prefix").toInstance(ZConfig.getString("notification.topic-prefix"))
    bind[String].annotatedWithName("notification-mapping-file").toInstance(ZConfig.getString("notification.es.mapping-file"))

    bind[String].annotatedWithName("one-signal-url").toInstance(ZConfig.getString("one-signal.url"))
    bind[String].annotatedWithName("one-signal-id").toInstance(ZConfig.getString("one-signal.id"))
    bind[String].annotatedWithName("one-signal-key").toInstance(ZConfig.getString("one-signal.key"))

    bind[Config].annotatedWithName("push-notification-consumer-config").toInstance(ZConfig.getConf("notification.kafka.consumers.push"))
    bind[Config].annotatedWithName("email-notification-consumer-config").toInstance(ZConfig.getConf("notification.kafka.consumers.email"))
    bind[Config].annotatedWithName("notification-writer-config").toInstance(ZConfig.getConf("notification.kafka.consumers.writer"))
    bind[Config].annotatedWithName("smtp-config").toInstance(ZConfig.getConf("smtp"))

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

  @Provides
  @Singleton
  def providesNotifyKafkaProducer: StringKafkaProducer = StringKafkaProducer(ZConfig.getConf("notification.kafka.producer"))

}
