package circe.ccp.notification.util

import java.io.File
import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.collection.JavaConversions._

/**
 * Created by phg on 10/3/16.
 **/
object JsonUtil extends Jsoning

trait Jsoning {

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)


  def fromJson[T: Manifest](json: String): T = mapper.readValue[T](json, typeReference[T])

  def fromJson[T: Manifest](file: File): T = mapper.readValue[T](file, typeReference[T])

  def readTree(json: String): JsonNode = mapper.readTree(json)

  def readFile(file: File): JsonNode = mapper.readTree(file)

  def toJson[T](t: T): String = {
    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
  }

  def createObjectNode = mapper.createObjectNode

  def createArrayNode = mapper.createArrayNode

  private[this] def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType: Type = typeFromManifest(manifest[T])
  }

  private[this] def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) {
      m.runtimeClass
    }
    else new ParameterizedType {
      def getRawType = m.runtimeClass

      def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray

      def getOwnerType = null

    }
  }

  implicit class JsonObject(map: Any) {
    def toJsonString = JsonUtil.toJson(map)

    def toJsonObject[A: Manifest] = map.toJsonString.asJsonObject[A]
  }

  implicit class ObjectLike(str: String) {
    def asJsonObject[A: Manifest] = JsonUtil.fromJson[A](str)

    def asJsonNode = JsonUtil.readTree(str)
  }

  implicit class JsonNodeLike(node: JsonNode) {
    def asArrayOfNode: Array[JsonNode] = node.asInstanceOf[ArrayNode].elements().toArray

    def asArrayOfString: Array[String] = if (node.isMissingNode) Array[String]() else node.asArrayOfNode.map(_.asText(""))

    def toJsonString: String = JsonUtil.toJson(node)

    def asMap: Map[String, Any] = node.toJsonString.asJsonObject[Map[String, Any]]
  }

}