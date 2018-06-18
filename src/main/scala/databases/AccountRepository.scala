package databases

import com.fullfacing.apollo.data.mongo.environment.MongoConfiguration
import com.fullfacing.apollo.data.mongo.operations.MongoCrudService
import com.mongodb.client.model.changestream.FullDocument
import interfaces.Indexable.IAccount
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import com.fullfacing.apollo.data.mongo.utils.MongoImplicits._
import interfaces.ChangeStreamHelper
import monix.reactive.Observable
import org.mongodb.scala.{ChangeStreamObservable, Document, MongoCollection}

trait AccountRepository {
  self: MongoConfiguration =>
  val cAccounts: AccountCollection

  class AccountCollection extends MongoCrudService[IAccount] {

    override val collection: Task[MongoCollection[Document]] = database.map(_.getCollection("accounts")).memoizeOnSuccess

    /**
      * Initiates the change stream for the collection
      */
    def initChangeStream(): Unit = {
      val cursor: Task[ChangeStreamObservable[Document]] = collection.map(_.watch().fullDocument(FullDocument.UPDATE_LOOKUP))

      Observable.fromTask(cursor)
        .flatMap(c => Observable.fromReactivePublisher(c.toPublisher))
        .onErrorRestartUnlimited
        .subscribe(doc =>
          ChangeStreamHelper.onNext(doc),
          (ex: Throwable) => println(s" ${Console.RED} There was an error: $ex"),
          () => println("Complete"))
    }
  }
}
