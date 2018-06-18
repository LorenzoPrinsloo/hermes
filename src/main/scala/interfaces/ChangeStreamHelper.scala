package interfaces

import api.ElasticApi
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.changestream.ChangeStreamDocument
import handles.Monix.completeTask
import interfaces.Indexable.IAccount
import com.fullfacing.apollo.data.mongo.logger
import org.mongodb.scala.bson.collection.immutable.Document
import com.fullfacing.apollo.data.mongo.utils.MongoImplicits._
import monix.execution.Scheduler.Implicits.global
import com.fullfacing.common.tcs.serializers.SerializationFormats.formats
import handles.mongo.MongoParser
import handles.mongo.MongoParser.DocumentParser
import monix.execution.Ack
import monix.execution.Ack.Continue

import scala.concurrent.Future

object ChangeStreamHelper {
  implicit val im: DocumentParser[IAccount, Document] = MongoParser.create[IAccount]

  def onNext(doc: ChangeStreamDocument[Document]): Future[Ack] = {

    doc.getOperationType.getValue match {
      case "replace" => replaceExistingIndex(doc)
      case "insert" => storeNewIndex(doc)
      case "update" => updateExistingIndex(doc)
      case "delete" => deleteIndex(doc)
      case _ => logger.info(s"${Console.MAGENTA} Unsupported change event")
    }

    Continue
  }

  private def replaceExistingIndex(doc: ChangeStreamDocument[Document]): Unit = {

    val replacedId: String = doc.getFullDocument.get("id").getOrElse(BsonString("")).asString().getValue

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}: $replacedId")
    logger.info(s"${Console.GREEN} Data: ${doc.getFullDocument}")

    completeTask(ElasticApi.updateExistingIndex(replacedId, "bank/account", im.fromDocument(doc.getFullDocument)))

  }

  private def updateExistingIndex(doc: ChangeStreamDocument[Document]): Unit = {
    val updatedId: String = doc.getFullDocument.get("id").getOrElse(BsonString("")).asString().getValue

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Updated: ${doc.getUpdateDescription.getUpdatedFields}")
    logger.info(s"${Console.GREEN} Removed: ${doc.getUpdateDescription.getRemovedFields}")

    completeTask(ElasticApi.updateExistingIndex(updatedId, "bank/account", im.fromDocument(doc.getFullDocument)))
  }

  private def storeNewIndex(doc: ChangeStreamDocument[Document]): Unit = {

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Data: ${doc.getFullDocument}")

    completeTask(ElasticApi.storeIntoIndex(im.fromDocument(doc.getFullDocument), "bank/account"))
    //TODO fix Implicit Task Converters
  }

  private def deleteIndex(doc: ChangeStreamDocument[Document]): Unit = {
    val deletedId: String = doc.getDocumentKey.get("_id").toString

    logger.info(s"${Console.MAGENTA} ${doc.getOperationType.getValue.toUpperCase}")
    logger.info(s"${Console.GREEN} Deleted: $deletedId")

    completeTask(ElasticApi.deleteFromIndex(deletedId, "bank/account"))
    //TODO add static-ly defined enum Mappings for collectionName to Elastic index name. Perhaps store in Mongo Meta Data obj?
  }
}
