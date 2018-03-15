package circe.ccp.notification.consumer

import java.util.Properties
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}

import circe.ccp.notification.domain.{EmailNotificationInfo, Notification}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.{Config, ConfigRenderOptions}

import scala.collection.JavaConversions._
import scala.language.implicitConversions

/**
 * Created by phg on 3/14/18.
 **/
case class EmailNotificationDeliverer @Inject()(
  @Named("email-notification-consumer-config") config: Config,
  @Named("smtp-config") smtpConfig: Config
) extends NotificationConsumer(config) {

  private def properties = {
    val props = new Properties()
    smtpConfig.entrySet().foreach(entry => props.put(entry.getKey, entry.getValue.unwrapped().toString))
    props
  }

  private def authenticator = {
    val username = smtpConfig.getString("username")
    val password = smtpConfig.getString("password")
    new Authenticator() {
      override protected def getPasswordAuthentication = new PasswordAuthentication(username, password)
    }
  }

  implicit def S2IA(s: String): InternetAddress = new InternetAddress(s)

  override def handleCreate(notification: Notification): Unit = {
    val info = notification.data.asJsonObject[EmailNotificationInfo]

    val msg = new MimeMessage(Session.getInstance(properties, authenticator))

    msg.setFrom(notification.sender)

    msg.addRecipient(Message.RecipientType.TO, notification.receiver)
    info.to.foreach(msg.addRecipient(Message.RecipientType.TO, _))
    info.cc.foreach(msg.addRecipient(Message.RecipientType.CC, _))
    info.bcc.foreach(msg.addRecipient(Message.RecipientType.BCC, _))

    msg.setSubject(info.subject, "UTF-8")
    msg.setText(info.body, "UTF-8", "html")

    Transport.send(msg)
  }
}
