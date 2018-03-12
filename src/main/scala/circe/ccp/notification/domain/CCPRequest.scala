package circe.ccp.notification.domain

import com.twitter.finatra.request.{QueryParam, RouteParam}

/**
 * Created by phg on 3/12/18.
 **/
class PagingRequesting {
  val page: Int = 1
  val size: Int = 10
  val sorts: Option[String] = None

  def getPageable: Pageable = PageNumberRequest(page, if (size > 1000) 1000 else size)

  def getSorts: Array[String] = sorts.getOrElse("").split(",").filter(_.nonEmpty)
}

case class PagingRequest(
  @QueryParam override val page: Int = 1,
  @QueryParam override val size: Int = 10,
  @QueryParam override val sorts: Option[String] = None
) extends PagingRequesting

case class AddNotificationRequest(
  sender: String,
  receiver: String,
  notifyType: String,
  data: String
)

case class GetNotificationRequest(
  @RouteParam receiver: String,
  @QueryParam isRead: Option[Boolean] = None,
  @QueryParam notifyType: Option[String] = None,
  @QueryParam override val page: Int = 1,
  @QueryParam override val size: Int = 10,
  @QueryParam override val sorts: Option[String] = None
) extends PagingRequesting
