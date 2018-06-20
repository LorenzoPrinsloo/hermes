package api

import cats.implicits._
import com.fullfacing.apollo.core.protocol.internal.ErrorPayload
import com.fullfacing.apollo.core.protocol.{ResponseCode, SingleResponse}
import com.fullfacing.common.tcs.serializers.SerializationFormats.formats
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.index.RichIndexResponse
import com.sksamuel.elastic4s.mappings.{GetMappingsResult, MappingDefinition}
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.update.RichUpdateResponse
import handles.Elastic.client
import interfaces.Indexable.ElasticDomainModel
import monix.eval.Task
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.index.reindex.BulkByScrollResponse
import org.json4s.jackson.Serialization.read

import scala.util.{Failure, Success}

object ElasticApi {

  private case class IndexAndTypeDefinedResponse(var indx: Boolean = false, var `type`: Boolean = false)

  def searchQ(indx: IndexAndType, searchQuery: String): Task[Either[ErrorPayload, SingleResponse[RichSearchResponse]]] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute(search(indx) query searchQuery)
      )
    ).map(res => SingleResponse(res, ResponseCode.Ok, "Ok").asRight)
  }

  def storeIntoIndex(itemId: String, json: String, iName: IndexAndType): Task[RichIndexResponse] = {
    val map = read[Map[String, Any]](json)
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          indexInto(iName) fields map id itemId refresh RefreshPolicy.IMMEDIATE
        }
      )
    )
  }

  def deleteFromIndex(id: String, indx: IndexAndType): Task[BulkByScrollResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          deleteIn(indx).by(termQuery("id", id))
        }
      )
    )
  }

  def updateExistingIndex[A <: ElasticDomainModel](id: String, indx: IndexAndType, json: String): Task[RichUpdateResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          update(id).in(indx).doc(json)
        }
      )
    )
  }


  /**
    * Creates index for Elastic Searches
    *
    * @param indx : Name of the index
    * @param map  : Definition of the index as mappings
    *             MappingDefinition("accounts")
    *             .as(
    *             BasicFieldDefinition("name", "keyword")
    *             )
    * @return Task[CreateIndexResponse]
    */
  def createNewIndex(indx: String, map: MappingDefinition): Task[CreateIndexResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          createIndex(indx).mappings(map)
        }
      )
    )
  }

  /**
    * Creates new type and mapping for a collection under a certain index
    *
    * @param indx defines the index to store the new type and mapping aswell as the name of the mapping
    * @param json the raw json source from which a dynamic mapping will be generated on Elastic
    * @return Task[PutMappingResponse]
    */
  def createMapping(indx: IndexAndType, json: String): Task[PutMappingResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          putMapping(indx).rawSource(json)
        }
      )
    )
  }

  /**
    * Checks whether index and mapping for IndexAndType exists and builds a result containing that info
    *
    * @param indx IndexAndType to check
    * @return Task[IndexAndTypeExistsResponse]
    */
  def checkIndexAndTypeExists(indx: IndexAndType): Task[IndexAndTypeDefinedResponse] = {
    checkIndexExists(indx.index)
      .map(index => {
        val response: IndexAndTypeDefinedResponse = IndexAndTypeDefinedResponse(indx = index.isExists)
        if (index.isExists) {
          checkMappingExists(indx).runOnComplete {
            case Success(_) => response.`type` = true
            case Failure(_) => response.`type` = false
          }
        }
        response
      }
      )
  }

  def checkIndexExists(indx: String): Task[IndicesExistsResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          indexExists(indx)
        }
      )
    )
  }

  def checkMappingExists(indx: IndexAndType): Task[GetMappingsResult] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          getMapping(indx)
        }
      )
    )
  }

  def health(): Task[Either[ErrorPayload, SingleResponse[ClusterHealthResponse]]] =
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute(clusterHealth())
      )
    ).map(res => SingleResponse(res, ResponseCode.Ok, "Ok").asRight)
}