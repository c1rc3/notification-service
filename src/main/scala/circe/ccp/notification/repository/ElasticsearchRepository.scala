package circe.ccp.notification.repository

import java.io.File
import java.net.InetAddress

import circe.ccp.notification.domain.Pageable
import circe.ccp.notification.repository.ESClient._
import circe.ccp.notification.util.Jsoning
import com.twitter.util.{Future, Promise}
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.{GetResponse, MultiGetResponse}
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.support.PlainListenableActionFuture
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.action.{ActionRequest, ActionRequestBuilder, ActionResponse}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.{SortBuilder, SortBuilders, SortOrder}
import org.elasticsearch.threadpool.ThreadPool

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.language.implicitConversions

trait ESClient {

  implicit class ActionRequestBuilderAsyncGet[Req <: ActionRequest[Req], Resp <: ActionResponse, B <: ActionRequestBuilder[Req, Resp, B]](builder: ActionRequestBuilder[Req, Resp, B]) {

    def execAsync: Future[Resp] = {
      val promise = Promise[Resp]()
      builder.execute(new PlainListenableActionFuture[Resp](currentThreadPool) {
        override def onFailure(e: Throwable): Unit = promise.setException(e)

        override def onResponse(result: Resp): Unit = promise.setValue(result)
      })
      promise
    }

    private def currentThreadPool: ThreadPool = findThreadPool(builder.getClass)

    @tailrec // extract threadPool from ActionRequestBuilder
    private def findThreadPool(cls: Class[_]): ThreadPool = if (cls.getSimpleName.equals("ActionRequestBuilder")) {
      val field = cls.getDeclaredField("threadPool")
      field.setAccessible(true)
      field.get(builder).asInstanceOf[ThreadPool]
    } else findThreadPool(cls.getSuperclass)
  }

}

trait Elasticsearchable extends ESClient with Jsoning {
  protected val es: ElasticsearchRepository

  protected def initIndexFromJsonFile(file: String): Unit = {
    val json = readFile(new File(file))
    val settings = json.path("settings").asText("")
    val mappings = json.path("mappings").fields().toSeq.map(f => {
      (f.getKey, f.getValue.toJsonString)
    })
    es.Admin.initIndex(settings)
    mappings.foreach(f => {
      es.Admin.updateMapping(f._1, f._2)
    })
  }

  implicit class OptionQueryBuilder(queryBuilder: BoolQueryBuilder) {
    def mustTerm(key: String, value: Option[_]): BoolQueryBuilder = {
      value match {
        case Some(v) => queryBuilder.must(QueryBuilders.termQuery(key, v))
        case _ => ;
      }
      queryBuilder
    }

    def mustTerms(key: String, value: Option[String]): BoolQueryBuilder = {
      value match {
        case Some(v) => queryBuilder.must(QueryBuilders.termsQuery(key, v.split(","): _*))
        case _ => ;
      }
      queryBuilder
    }

    def mustRange(key: String, gte: Option[_] = None, lte: Option[_] = None, gt: Option[_] = None, lt: Option[_] = None) = {
      if (gte.isDefined || lte.isDefined || gt.isDefined || lt.isDefined) {
        val range = QueryBuilders.rangeQuery(key)
        gte match {
          case Some(g) => range.gte(g)
          case _ => ;
        }
        lte match {
          case Some(l) => range.lte(l)
          case _ => ;
        }
        gt match {
          case Some(g) => range.gt(g)
          case _ => ;
        }
        lt match {
          case Some(l) => range.lt(l)
          case _ => ;
        }
        queryBuilder.must(range)
      }
      queryBuilder
    }
  }

  implicit def SearchResponse2ArrayObject[A: Manifest](searchResponse: SearchResponse): Array[A] = {
    searchResponse.getHits.getHits.map(_.getSourceAsString.asJsonObject[A])
  }
}

object ESClient extends ESClient

case class ElasticsearchRepository(servers: List[String], cluster: String, transportSniff: Boolean, indexName: String) {

  protected val client: TransportClient = {
    val cli = TransportClient.builder().settings(
      Settings.builder()
        .put("cluster.name", cluster)
        .put("client.transport.sniff", transportSniff)
        .build()
    ).build()

    // add transport
    servers.map(_.split(":")).filter(_.length == 2).foreach(hp => {
      cli.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hp(0)), hp(1).toInt))
    })
    cli
  }

  def getClient: TransportClient = client

  object Admin {

    def isExistsIndex(index: String): Future[Boolean] = {
      client.prepareExists(index).execAsync.map(_.exists())
    }

    def initIndex(setting: String, mappings: Seq[(String, String)] = Seq()): Unit = {
      if (!client.admin().indices().prepareExists(indexName).get().isExists) {
        val prepareIndex = client.admin().indices().prepareCreate(indexName)
          .setSettings(setting)
        mappings.foreach(f => {
          prepareIndex.addMapping(f._1, f._2)
        })
        prepareIndex.get()
      }
    }

    def registerMapping(index: String, types: String, mapping: String): Future[PutMappingResponse] = {
      client.admin().indices()
        .preparePutMapping(index)
        .setType(types)
        .setSource(mapping)
        .execAsync
    }

    def updateMapping(types: String, mapping: String): PutMappingResponse = {
      client.admin().indices()
        .preparePutMapping(indexName)
        .setType(types)
        .setSource(mapping)
        .execute().actionGet()
    }
  }

  def get(`type`: String, id: String): Future[GetResponse] = client.prepareGet(indexName, `type`, id).execAsync

  def getSync(`type`: String, id: String): GetResponse = client.prepareGet(indexName, `type`, id).execute().actionGet()

  def mget(`type`: String, ids: Array[String]): Future[MultiGetResponse] = client.prepareMultiGet().add(indexName, `type`, ids: _*).execAsync

  def index(`type`: String, id: String, source: String): Future[IndexResponse] = client.prepareIndex(indexName, `type`, id).setSource(source).execAsync

  def indexSync(`type`: String, id: String, source: String): IndexResponse = client.prepareIndex(indexName, `type`, id).setSource(source).execute().actionGet()

  def update(`type`: String, id: String, doc: String): Future[UpdateResponse] = client.prepareUpdate(indexName, `type`, id).setDoc(doc).execAsync

  def delete(`type`: String, id: String): Future[DeleteResponse] = client.prepareDelete(indexName, `type`, id).execAsync

  def upsert(`type`: String, id: String, doc: String): Future[UpdateResponse] = {
    client.prepareUpdate(indexName, `type`, id).setDoc(doc).setUpsert(doc).execAsync
  }

  def mupsert(`type`: String, data: Array[(String, String)]): Future[BulkResponse] = {
    val bulk = client.prepareBulk()
    data.foreach(f => {
      val id = f._1
      val doc = f._2
      bulk.add(client.prepareUpdate(indexName, `type`, id).setDoc(doc).setUpsert(doc))
    })
    bulk.execAsync
  }

  def prepareSearch = client.prepareSearch(indexName)

  def prepareCount = client.prepareCount(indexName)

  def fetchAll(types: String, queryBuilders: QueryBuilder)(f: SearchHit => Unit): Unit = _fetchAll(types, queryBuilders)(f)

  private def _fetchAll(types: String, queryBuilders: QueryBuilder, fetchSource: Boolean = true, sorts: Array[SortBuilder] = Array())(f: SearchHit => Unit): Unit = {
    val req = client.prepareSearch(indexName).setTypes(types)
      .setSize(10)
      .setQuery(queryBuilders)
      .setScroll(new TimeValue(60000))
      .setFetchSource(fetchSource)
    sorts.foreach(req.addSort)
    val res = req.execute().actionGet()
    _process(res)(f)
    _scroll(res.getScrollId)(f)
  }

  def fetchAll(types: String)(f: SearchHit => Unit): Unit = {
    val res = client.prepareSearch(indexName).setTypes(types)
      .setSize(10)
      .setScroll(new TimeValue(60000))
      .execute().actionGet()
    _process(res)(f)
    _scroll(res.getScrollId)(f)
  }

  @tailrec
  private[this] def _scroll(id: String)(f: SearchHit => Unit): Unit = {
    val res = client.prepareSearchScroll(id).setScroll(new TimeValue(60000)).execute.actionGet
    _process(res)(f)
    if (res.getHits.getHits.nonEmpty) _scroll(res.getScrollId)(f)
  }

  private[this] def _process(res: SearchResponse)(f: SearchHit => Unit) = for (hit <- res.getHits.getHits) f(hit)

  def getAllIds(types: String): Seq[String] = {
    var res = prepareSearch.setTypes(types)
      .setFetchSource(false)
      .setSize(1000)
      .setScroll(new TimeValue(60000))
      .execute().actionGet()
    var ids: Seq[String] = res.getHits.getHits.map(_.getId)
    while (res.getHits.getHits.nonEmpty && null != res.getScrollId) {
      res = client.prepareSearchScroll(res.getScrollId).execute().actionGet()
      ids = ids ++ res.getHits.getHits.map(_.getId)
    }
    ids
  }

  def fetchId(types: String, queryBuilder: QueryBuilder)(fn: String => Unit): Unit = {
    var res = prepareSearch.setTypes(types)
      .setFetchSource(false)
      .setQuery(queryBuilder)
      .setSize(1000)
      .setScroll(new TimeValue(60000))
      .execute().actionGet()
    var ids: Seq[String] = res.getHits.getHits.map(_.getId)
    while (res.getHits.getHits.nonEmpty && null != res.getScrollId) {
      res = client.prepareSearchScroll(res.getScrollId).execute().actionGet()
      ids = ids ++ res.getHits.getHits.map(_.getId)
    }
    ids.foreach(fn)
  }

  def search(`type`: String, query: QueryBuilder, pageable: Pageable): Future[SearchResponse] = {
    val req = prepareSearch.setTypes(`type`)
      .setQuery(query)
      .setFrom(pageable.from)
      .setSize(pageable.size)
    pageable.sorts.foreach(sort => {
      val order = if (sort.startsWith("-")) SortOrder.DESC else SortOrder.ASC
      val field = if (sort.startsWith("-") || sort.startsWith("+")) sort.substring(1) else sort
      req.addSort(SortBuilders.fieldSort(field).order(order))
    })
    req.execAsync
  }

}