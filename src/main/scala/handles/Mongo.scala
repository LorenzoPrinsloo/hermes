package handles

import api.ElasticApi
import com.mongodb.client.model.changestream.FullDocument
import interfaces.Indexable.Account
import org.mongodb.scala.{MongoClient, ScalaObservable}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.changestream._
import org.mongodb.scala.model.Aggregates._
import main.Main.logger
import api.Http.formats
import handles.mongo.MongoParser
import monix.execution.Scheduler.Implicits.global
import handles.mongo.MongoParser.DocumentParser
import handles.Monix.completeTask

object Mongo {

  implicit val im: DocumentParser[Account, Document] = MongoParser.create[Account]

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
        case "replace" => replaceExistingIndex(doc)
        case "insert" => storeNewIndex(doc)
        case "update" => updateExistingIndex(doc)
        case "delete" => deleteIndex(doc)
        case _ => logger.info(s"${Console.MAGENTA} Unsupported change event")
      }
    }
  }

  private def replaceExistingIndex(doc: ChangeStreamDocument[Document]): Unit = {
    val replacedId: String = doc.getDocumentKey.get("_id").asObjectId().getValue.toString

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Data: ${doc.getFullDocument}")

    completeTask(ElasticApi.updateExistingIndex(replacedId, "bank/account", im.fromDocument(doc.getFullDocument)))

  }

  private def updateExistingIndex(doc: ChangeStreamDocument[Document]): Unit = {
    val updatedId: String = doc.getDocumentKey.get("_id").asObjectId().getValue.toString

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Updated: ${doc.getUpdateDescription.getUpdatedFields}")
    logger.info(s"${Console.GREEN} Removed: ${doc.getUpdateDescription.getRemovedFields}")

    completeTask(ElasticApi.updateExistingIndex(updatedId, "bank/account", im.fromDocument(doc.getFullDocument)))
    //TODO check if works with only updated fields passed through
  }

  private def storeNewIndex(doc: ChangeStreamDocument[Document]): Unit = {

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Data: ${doc.getFullDocument}")

    completeTask(ElasticApi.storeIntoIndex(im.fromDocument(doc.getFullDocument), "bank/account"))
    //TODO fix Implicit Task Converters
  }

  private def deleteIndex(doc: ChangeStreamDocument[Document]): Unit = {
    val deletedId: String = doc.getDocumentKey.get("_id").asObjectId().getValue.toString //TODO wrong id real index id is created on storing of new index item

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Deleted: $deletedId")

    completeTask(ElasticApi.deleteFromIndex(deletedId, "bank/account"))
    //TODO add staticly defined enum Mappings for collectionName to Elastic index name. Perhaps store in Mongo Meta Data obj?
  }
}
