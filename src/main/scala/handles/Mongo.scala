package handles

import com.fullfacing.apollo.core.internal.ResourceHandle
import com.fullfacing.apollo.data.mongo.environment.{MongoConfiguration, MongoConnection}
import databases.AccountRepository
import monix.eval.Task
import org.mongodb.scala.{MongoDatabase, ServerAddress}

object Mongo extends MongoConfiguration with ResourceHandle with AccountRepository {

  private val addresses: List[ServerAddress] = List(ServerAddress(sys.env.getOrElse("MONGODB_HOST", "localhost")))

  final val connection = new MongoConnection(addresses)
  override protected val database: Task[MongoDatabase] = {
    connection.getDatabase(sys.env.getOrElse("MONGODB_DB_NAME","test"))
  }

  override implicit val cAccounts: Mongo.AccountCollection = new AccountCollection()

  override def connect(): Task[Unit] = database.map(_ => cAccounts.initChangeStream())
}
