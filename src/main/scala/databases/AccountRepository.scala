package databases

import com.fullfacing.apollo.data.mongo.environment.MongoConfiguration
import com.fullfacing.apollo.data.mongo.operations.MongoCrudService
import com.mongodb.client.model.changestream.FullDocument
import interfaces.Indexable.Account
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import com.fullfacing.apollo.data.mongo.utils.MongoImplicits._
import interfaces.ChangeStreamSync
import monix.execution.atomic.Atomic
import monix.reactive.{Observable, Observer}
import org.mongodb.scala.{ChangeStreamObservable, Document, MongoCollection}
import org.reactivestreams.Publisher

trait AccountRepository {
  self: MongoConfiguration =>
  val cAccounts: AccountCollection

  class AccountCollection extends ChangeStreamSync[Account]("bank/account") with MongoCrudService[Account]  {

    override val collection: Task[MongoCollection[Document]] = database.map(_.getCollection("accounts")).memoizeOnSuccess

    /**
      * Initiates the change stream for the collection
      */
    def initStream(): Unit = {
      val cursor: Task[ChangeStreamObservable[Document]] = collection.map(_.watch().fullDocument(FullDocument.UPDATE_LOOKUP))

      Observable.fromTask(cursor)
        .flatMap(c => Observable.fromReactivePublisher(c.toPublisher))
        .onErrorRestartUnlimited
        .subscribe(onNext(_))
    }

  }
}
