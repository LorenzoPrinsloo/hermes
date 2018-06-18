import api.HttpApi
import com.fullfacing.apollo.rest.service.ApolloService
import handles.Akka._
import handles.Mongo
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main extends App {

  val server =
    for {
      _ <- Mongo.connect()
    } yield ApolloService.run(HttpApi.api)

    Await.ready(server.runAsync, Duration.Inf)
}
