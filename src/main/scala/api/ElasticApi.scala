package api

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.index.RichIndexResponse
import com.sksamuel.elastic4s.searches.RichSearchResponse
import monix.eval.Task
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import handles.Elastic.client
import interfaces.Indexable.ElasticIndexable
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.json4s.jackson.Serialization.{read, write}
import api.Http.formats
import com.sksamuel.elastic4s.update.RichUpdateResponse
import org.elasticsearch.action.delete.DeleteResponse
import handles.Elastic.classToIndexable

object ElasticApi {

  def searchQ(indx: IndexAndType, searchQuery: String): Task[RichSearchResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute(search(indx) query searchQuery)
      )
    )
  }

  def storeIntoIndex[A <: ElasticIndexable](indexItem: A, iName: IndexAndType): Task[RichIndexResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          indexInto(iName) fields read[Map[String, Any]](write(indexItem)) refresh RefreshPolicy.IMMEDIATE
        }
      )
    )
  }

  def deleteFromIndex(id: String, indx: IndexAndType): Task[DeleteResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
          delete(id).from(indx)
        }
      )
    )
  }

  def updateExistingIndex[A <: ElasticIndexable](id: String, indx: IndexAndType, document: A): Task[RichUpdateResponse] = {
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute {
         update(id).in(indx).doc(document)(classToIndexable(document))
        }
      )
    )
  }

  def createIndex(): Unit = ???

  def health(): Task[ClusterHealthResponse] =
    client.flatMap(cl =>
      Task.deferFuture(
        cl.execute(clusterHealth())
      )
    )
}