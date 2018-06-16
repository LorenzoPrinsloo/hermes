package handles

import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success}

object AkkaHttp {

  implicit def completeTask[A <: AnyRef](x: Task[A])(implicit formats: Formats, scheduler: Scheduler): Route = {
    onComplete(x.runAsync) {
      case Success(res) => complete(write(res))
      case Failure(err) => complete(err.getMessage)
    }
  }
}
