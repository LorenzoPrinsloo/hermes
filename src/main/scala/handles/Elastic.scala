package handles

import com.sksamuel.elastic4s.{ElasticsearchClientUri, Indexable, TcpClient}
import monix.eval.Task
import org.elasticsearch.common.settings.Settings
import org.json4s.Formats
import org.json4s.jackson.Serialization.write

object Elastic {

  val settings = Settings
    .builder()
    .put("cluster.name", "elasticsearch")
    .put("client.transport.sniff", false)
    .build()

  lazy val client = Task(TcpClient.transport(settings, ElasticsearchClientUri("localhost", 9300))).memoizeOnSuccess

  //TODO move conversion to ElasticIndexable Trait
  def classToIndexable[A <: AnyRef](item: A)(implicit formats: Formats): Indexable[A] = {
    new Indexable[A] {
      override def json(t: A): String = write(item)
    }
  }
}
