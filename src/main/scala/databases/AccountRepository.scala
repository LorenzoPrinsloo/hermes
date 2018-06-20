package databases

import com.fullfacing.apollo.data.mongo.environment.MongoConfiguration
import com.fullfacing.apollo.data.mongo.operations.MongoCrudService
import interfaces.ChangeStreamSync
import interfaces.Indexable.Account
import monix.eval.Task
import org.mongodb.scala.{Document, MongoCollection}

trait AccountRepository {
  self: MongoConfiguration =>
  val cAccounts: AccountCollection

  class AccountCollection extends ChangeStreamSync[Account]("tacos/account") with MongoCrudService[Account]  {

    override val collection: Task[MongoCollection[Document]] = database.map(_.getCollection("accounts")).memoizeOnSuccess

    initStream(collection)
  }
}
