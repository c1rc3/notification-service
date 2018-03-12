package circe.ccp.notification.domain

import circe.ccp.notification.domain.FailureReason.FailureReason
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.twitter.util.Future

/**
 * Created by phg on 3/8/18.
 **/
trait CCPResponse {
  val code: Int
  val msg: String
  val data: Option[Any]
}

case class StandardResponse(code: Int, msg: String, data: Option[Any] = None) extends CCPResponse

@JsonIgnoreProperties(value = Array("value"))
case class SuccessResponse(value: Any = null) extends CCPResponse {
  override val code: Int = 1
  override val msg: String = "OK"
  override val data = Option(value)
}

@JsonIgnoreProperties(value = Array("reason"))
case class FailureResponse(
  reason: FailureReason
) extends CCPResponse {
  override val code: Int = reason.id
  override val msg: String = reason.toString
  override val data: Option[Any] = None
}

@JsonIgnoreProperties(value = Array("page"))
case class PagingResponse(
  page: Page[_]
) extends CCPResponse {
  override val code: Int = 1
  override val msg: String = "OK"
  override val data = Option(Map(
    "content" -> page.content,
    "total_element" -> page.totalElement,
    "total_page" -> page.totalPage,
    "current_page" -> page.currentPage,
    "from" -> page.from,
    "size" -> page.size
  ))
}

object FailureReason extends Enumeration {
  type FailureReason = Value

  val NotFound = Value(-404, "not-found")
}

trait Response {

  implicit class FutureLike(any: Future[Any]) {

    def toCCPSuccessResponse: Future[CCPResponse] = {
      any.map {
        case None => FailureResponse(FailureReason.NotFound)
        case data: Page[_] => SuccessResponse(Option(data))
        case data@_ => SuccessResponse(Option(data))
      }
    }

    def toCCPFailureResponse(reason: FailureReason): Future[FailureResponse] = any.map(_ => FailureResponse(reason))
  }

}