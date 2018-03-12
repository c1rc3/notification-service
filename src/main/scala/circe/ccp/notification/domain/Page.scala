package circe.ccp.notification.domain

/**
 * Created by plt on 12/1/17.
 **/
trait Pageable {
  def from: Int

  def to: Int

  def page: Int

  def size: Int

  def first: Pageable

  def next: Pageable

  def previousOrFirst: Pageable

  def sorts: Array[String]
}

trait Page[T] {
  def totalElement: Long

  def totalPage: Int

  def content: Array[T]

  def scrollId: Option[String]

  def currentPage: Int

  def from: Int

  def size: Int
}

case class PageImpl[T](content: Array[T], pageable: Pageable, total: Long, scrollId: Option[String] = None) extends Page[T] {

  override def totalElement: Long = total

  override def totalPage: Int = math.ceil(total * 1.0f / pageable.size).toInt

  override def currentPage: Int = pageable.page

  override def from = pageable.from

  override def size = pageable.size
}

case class PageNumberRequest(page: Int, size: Int, sorts: Array[String] = Array.empty) extends Pageable {

  override def from: Int = if (page > 1) (page - 1) * size else 0

  override def to = from + size

  override def first: Pageable = PageNumberRequest(1, size)

  override def next: Pageable = PageNumberRequest(page + 1, size)

  override def previousOrFirst: Pageable = if (page >= 2) PageNumberRequest(page - 1, size) else first
}

object DefaultPageRequest {
  def apply() = PageNumberRequest(1, 10)
}