package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import handles.AkkaHttp.completeHttpTask
import monix.execution.Scheduler.Implicits.global
import org.json4s.jackson.Serialization
import org.json4s.jackson.JsonMethods._
import org.json4s._


object Http {

  implicit val formats = DefaultFormats

  val route: Route =
    path("health") {
      get {
        ElasticApi.health()
      }
    } ~
   path("search") {
     get {
       parameters('query.as[String], 'indx.as[String])((searchQuery, indx) =>
         ElasticApi.searchQ(indx, searchQuery)
       )
     }
   }

}