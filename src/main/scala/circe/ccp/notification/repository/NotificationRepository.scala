package circe.ccp.notification.repository

import circe.ccp.notification.domain.NotificationType.NotificationType
import circe.ccp.notification.domain.{Notification, Page, PageImpl, Pageable}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.twitter.util.Future
import org.elasticsearch.index.query.QueryBuilders

/**
 * Created by phg on 3/14/18.
 **/
trait NotificationRepository {
  def add(id: String, source: String): Future[String]

  def get(id: String): Future[Option[Notification]]

  def update(id: String, doc: String): Future[Boolean]

  def search(sender: Option[String] = None, receiver: Option[String] = None, notifyType: Option[NotificationType] = None, isRead: Option[Boolean] = None, pageable: Pageable): Future[Page[Notification]]

  def count(sender: Option[String] = None, receiver: Option[String] = None, notifyType: Option[NotificationType] = None, isRead: Option[Boolean] = None): Future[Long]
}

case class ESNotificationRepository @Inject()(
  es: ElasticsearchRepository,
  @Named("notification-type-name") typeName: String,
  @Named("notification-mapping-file") filePath: String
) extends NotificationRepository with Elasticsearchable {

  initIndexFromJsonFile(filePath)

  override def add(id: String, source: String) = es.upsert(typeName, id, source).map(_ => id)

  override def get(id: String) = es.get(typeName, id).map(res => {
    if (res.isExists) Some(res.getSourceAsString.asJsonObject[Notification])
    else None
  })

  override def update(id: String, doc: String) = es.update(typeName, id, doc).map(_ => true)

  override def search(sender: Option[String] = None, receiver: Option[String] = None, notifyType: Option[NotificationType] = None, isRead: Option[Boolean] = None, pageable: Pageable) = {
    es.search(typeName,
      QueryBuilders.boolQuery()
        .mustTerm("sender", sender)
        .mustTerm("receiver", receiver)
        .mustTerm("notify_type", notifyType.map(_.toString))
        .mustTerm("is_read", isRead), pageable
    ).map(res => PageImpl(res, pageable, res.getHits.totalHits))
  }

  override def count(sender: Option[String] = None, receiver: Option[String] = None, notifyType: Option[NotificationType] = None, isRead: Option[Boolean] = None) = {
    es.prepareCount.setTypes(typeName).setQuery(QueryBuilders.boolQuery()
      .mustTerm("sender", sender)
      .mustTerm("receiver", receiver)
      .mustTerm("notify_type", notifyType.map(_.toString))
      .mustTerm("is_read", isRead)).execAsync.map(_.getCount)
  }
}