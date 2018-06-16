package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import handles.AkkaHttp.completeTask
import monix.execution.Scheduler.Implicits.global
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}


object Http {

  implicit val formats = Serialization.formats(NoTypeHints)

  val route: Route =
    path("health") {
      get {
        ElasticApi.health()
      }
    } ~
   path("search") {
     get {
       ElasticApi.searchT("bank/account")
     }
   }

}