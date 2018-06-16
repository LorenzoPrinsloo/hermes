package handles

import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import monix.eval.Task
import org.elasticsearch.common.settings.Settings

object Elastic {

  val settings = Settings
    .builder()
    .put("cluster.name", "elasticsearch")
    .put("client.transport.sniff", false)
    .build()

  lazy val client = Task(TcpClient.transport(settings, ElasticsearchClientUri("localhost", 9300))).memoizeOnSuccess
}
