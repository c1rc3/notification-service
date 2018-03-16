package circe.ccp.notification.domain

/**
 * Created by phg on 3/14/18.
 **/
object KafkaCommand extends Enumeration {
  type KafkaCommand = Value

  val CREATE = Value(1, "CREATE")

  val UPDATE = Value(2, "UPDATE")

  val DELETE = Value(3, "DELETE")

  def forName(s: String) = values.find(_.toString == s)
}
