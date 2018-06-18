package api

import akka.http.scaladsl.server.Directives._
import com.fullfacing.apollo.core.health.HealthCheck
import com.fullfacing.apollo.rest.http.directives.Directives.onCompleteWithResponse
import monix.execution.Scheduler.Implicits.global
import com.fullfacing.apollo.rest.service.ApolloHttpService
import com.fullfacing.apollo.rest.service.ApolloService._
import handles.Mongo
import com.fullfacing.apollo.rest.http.directives.magnets.OnCompleteWithResponseMagnet.onCompleteWithResponseStrict
import com.fullfacing.common.tcs.serializers.SerializationFormats.formats


object HttpApi extends ApolloHttpService("v1" - "elastic") {

  override val handles: List[HealthCheck] = List(Mongo.connection)

  override val routes: RequestHandler = { implicit context =>

    get {
      path("health") {
        onCompleteWithResponse(onCompleteWithResponseStrict(ElasticApi.health()))
      } ~
        path("search") {
          parameters('query.as[String], 'indx.as[String])((searchQuery, indx) =>
            onCompleteWithResponse(onCompleteWithResponseStrict(ElasticApi.searchQ(indx, searchQuery)))
          )

        } ~
        path("delete") {
          parameters('id.as[String], 'indx.as[String])((id, indx) =>
            onCompleteWithResponse(onCompleteWithResponseStrict(ElasticApi.deleteFromIndex(id, indx)))
          )
        }
    }

  }

}