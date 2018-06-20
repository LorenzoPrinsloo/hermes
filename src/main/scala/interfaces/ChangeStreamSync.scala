package interfaces

import api.ElasticApi
import com.fullfacing.apollo.data.mongo.logger
import com.fullfacing.apollo.data.mongo.utils.MongoImplicits._
import com.mongodb.client.model.changestream.FullDocument
import com.sksamuel.elastic4s.IndexAndType
import com.sksamuel.elastic4s.mappings.MappingDefinition
import handles.Monix.completeTask
import interfaces.Indexable.ElasticDomainModel
import monix.eval.Task
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.changestream.ChangeStreamDocument
import org.mongodb.scala.{ChangeStreamObservable, MongoCollection}

import scala.concurrent.Future
import scala.util.{Failure, Success}

abstract class ChangeStreamSync[A <: ElasticDomainModel](indx: IndexAndType) {

  /**
    * Initiates the change stream for the collection
    */
  def initStream(col: Task[MongoCollection[Document]]): Unit = {
    val cursor: Task[ChangeStreamObservable[Document]] = col.map(_.watch().fullDocument(FullDocument.UPDATE_LOOKUP))

    Observable.fromTask(cursor)
      .flatMap(c => Observable.fromReactivePublisher(c.toPublisher))
      .onErrorRestartUnlimited
//      .doAfterSubscribe(initIndex)
      .subscribe(onNext(_))
  }

//  def initIndex(): Unit = {
//    ElasticApi.checkIndexAndTypeExists(indx)
//      .map(isDefined =>
//        (isDefined.indx, isDefined.`type`) match {
//          case (true, true) => ()
//          case (false, false) => createIndex()
//          case (true, false) =>
//          }
//
//      )
//  }

  def onNext(doc: ChangeStreamDocument[Document]): Future[Ack] = {
    val json: String = doc.getFullDocument.dropWhile(field => field._1 == "_id").toJson()
    val id: String = doc.getFullDocument.get("id").getOrElse(BsonString("")).asString().getValue

    ElasticApi.checkIndexAndTypeExists(indx)
      .foreach(isDefined =>
        if (isDefined.indx && isDefined.`type`) {
          executeOp(doc.getOperationType.getValue, json, id, doc)
        }
        else {
          createIndex(json).runOnComplete {
            case Success(_) =>
              executeOp(doc.getOperationType.getValue, json, id, doc)
            case Failure(err) =>
              logger.error(err.getCause.getMessage)
          }
        }
      )

    Continue
  }

  private def executeOp(opType: String, json: String, id: String, doc: ChangeStreamDocument[Document]): Unit = {
    opType match {
      case "replace" => replaceExistingIndex(json, id)
      case "insert" => storeNewIndex(json, id)
      case "update" => updateExistingIndex(json, id, doc)
      case "delete" => deleteIndexItem(id)
      case _ => logger.info(s"${Console.MAGENTA} Unsupported change event")
    }
  }

  private def createIndex(json: String): Task[CreateIndexResponse] = {
    logger.info(s"${Console.MAGENTA} Creating index: $indx")
    logger.info(s"${Console.GREEN} Data: $json")

    ElasticApi.createNewIndex(indx.index, MappingDefinition(indx.`type`, rawSource = Some(json)))
  }

  private def replaceExistingIndex(json: String, id: String): Unit = {
    logger.info(s"${Console.MAGENTA} REPLACE: $id")
    logger.info(s"${Console.GREEN} Data: $json")

    completeTask(ElasticApi.updateExistingIndex(id, indx, json))
  }

  private def updateExistingIndex(json: String, id: String, doc: ChangeStreamDocument[Document]): Unit = {
    logger.info(s"${Console.MAGENTA} UPDATE: $id")
    logger.info(s"${Console.GREEN} Updated: ${doc.getUpdateDescription.getUpdatedFields}")
    logger.info(s"${Console.GREEN} Removed: ${doc.getUpdateDescription.getRemovedFields}")

    completeTask(ElasticApi.updateExistingIndex(id, indx, json))
  }

  private def storeNewIndex(json: String, id: String): Unit = {
    logger.info(s"${Console.MAGENTA} INSERT")
    logger.info(s"${Console.GREEN} Data: $json")

    completeTask(ElasticApi.storeIntoIndex(id, json, indx))
  }

  private def deleteIndexItem(id: String): Unit = {
    logger.info(s"${Console.MAGENTA} DELETE: $id")

    completeTask(ElasticApi.deleteFromIndex(id, indx))
  }
}
