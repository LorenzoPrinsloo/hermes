package handles

import com.mongodb.client.model.changestream.FullDocument
import org.mongodb.scala.{MongoClient, ScalaObservable}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.changestream._
import org.mongodb.scala.model.Aggregates._
import main.Main.logger

object Mongo {

  def connect(): Unit = {
    val mongoClient = MongoClient("mongodb://localhost:9600")//27017

    // Select the MongoDB database and collection to open the change stream against
    val db = mongoClient.getDatabase("test")

    val collection = db.getCollection("accounts")

    // Create $match pipeline stage for filtering cursor
    val pipeline = Seq(filter(Document("operationType" -> "update")))

    val cursor = collection.watch(/*pipeline*/).fullDocument(FullDocument.UPDATE_LOOKUP)

    //Shorthand syntax for creating Observer for ChangeStreamDocument
    cursor.subscribe((doc: ChangeStreamDocument[Document]) =>
      onNext(doc),
      (e: Throwable) => println(s"There was an error: $e"), //TODO make so that on error retries to connect to the change stream
      () => println("Completed!"))

    def onNext(doc: ChangeStreamDocument[Document]): Unit = {

      doc.getOperationType.getValue match {
        case "replace" => {
          logger.info(s"${Console.BLUE} ${doc.getOperationType.getValue.toUpperCase}")
          logger.info(s"${Console.GREEN} Data: ${doc.getFullDocument}")
        }
        case "insert" => {
          logger.info(s"${Console.BLUE} ${doc.getOperationType.getValue.toUpperCase}")
          logger.info(s"${Console.GREEN} Data: ${doc.getFullDocument}")
        }
        case "update" => {
          logger.info(s"${Console.BLUE} ${doc.getOperationType.getValue.toUpperCase}")
          logger.info(s"${Console.GREEN} Updated: ${doc.getUpdateDescription.getUpdatedFields}")
          logger.info(s"${Console.GREEN} Removed: ${doc.getUpdateDescription.getRemovedFields}")
        }
        case "delete" => {
          logger.info(s"${Console.BLUE} ${doc.getOperationType.getValue.toUpperCase}")
          logger.info(s"${Console.GREEN} Deleted: ${doc.getDocumentKey}")
        }
        case _ => logger.info(s"${Console.MAGENTA} Unsupported change event")
      }
    }

  }
}
