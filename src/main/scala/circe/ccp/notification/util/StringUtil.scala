package circe.ccp.notification.util

import java.security.MessageDigest

/**
 * Created by plt on 11/21/17.
 **/
object StringUtil {

  def genUniqueId = s"${System.currentTimeMillis}_${Random.randomNumericString(4)}"

  def md5(value: String): String = MessageDigest.getInstance("MD5").digest(value.getBytes("UTF-8")).map(0xFF & _).map {
    "%02x".format(_)
  }.foldLeft("") {
    _ + _
  }
}
