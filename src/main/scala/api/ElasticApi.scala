package api

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.index.RichIndexResponse
import com.sksamuel.elastic4s.searches.RichSearchResponse
import monix.eval.Task
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import handles.Elastic.client
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.json4s.jackson.Serialization.{read, write}
import cats.implicits._
import com.fullfacing.apollo.core.protocol.internal.ErrorPayload
import com.fullfacing.apollo.core.protocol.{DomainModel, ResponseCode, SingleResponse}
import com.sksamuel.elastic4s.update.RichUpdateResponse
import org.elasticsearch.action.delete.DeleteResponse
import handles.Elastic.classToIndexable
import com.sksamuel.elastic4s.mappings.MappingDefinition
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.json4s.{DefaultFormats, Formats}

object ElasticApi {

  implicit val formats: Formats = DefaultFormats

  def searchQ(indx: IndexAndType, searchQuery: String): Task[Either[ErrorPayload, SingleResponse[RichSearchResponse]]] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute(search(indx) query searchQuery)
      )
    ).map(res => SingleResponse(res, ResponseCode.Ok, "Ok").asRight)
  }

  def storeIntoIndex[A <: DomainModel](indexItem: A, iName: IndexAndType): Task[RichIndexResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          indexInto(iName) fields read[Map[String, Any]](write(indexItem)) id indexItem.id.getOrElse("") refresh RefreshPolicy.IMMEDIATE
        }
      )
    )
  }

  def deleteFromIndex(id: String, indx: IndexAndType): Task[Either[ErrorPayload, SingleResponse[DeleteResponse]]] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          delete(id).from(indx)
        }
      )
    ).map(res => SingleResponse(res, ResponseCode.Ok, "Ok").asRight)
  }

  def updateExistingIndex[A <: DomainModel](id: String, indx: IndexAndType, document: A): Task[RichUpdateResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          update(id).in(indx).doc(document)(classToIndexable(document))
        }
      )
    )
  }


  /**
    * Creates index for Elastic Searches
    *
    * @param indx : Name of the index
    * @param map : Definition of the index as mappings
    *            MappingDefinition("accounts")
    *             .as(
    *             BasicFieldDefinition("name", "keyword")
    *             )
    * @return Task[CreateIndexResponse]
    */
  def createNewIndex(indx: IndexAndType, map: Seq[MappingDefinition]): Task[CreateIndexResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          createIndex(indx.toString).mappings(map)
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