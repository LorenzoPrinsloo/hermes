package interfaces

import java.time.OffsetDateTime

import com.fullfacing.apollo.core.protocol.DomainModel
import com.sksamuel.elastic4s.mappings.{BasicFieldDefinition, MappingDefinition}
import org.mongodb.scala.bson.ObjectId

object Indexable {

  case class IAccount(account_number: Double,
                      balance: Double,
                      firstName: String,
                      lastName: String,
                      age: Double,
                      gender: String,
                      address: String,
                      employer: String,
                      email: String,
                      city: String,
                      state: String,
                      createdAt: Option[OffsetDateTime] = None,
                      updatedAt: Option[OffsetDateTime] = None,
                      updatedBy: Option[String] = None,
                      deleted: Boolean = false,
                      id: Option[String] = None
                     ) extends DomainModel

  sealed abstract class Indexed(val indx: String) {
    def mapping(): MappingDefinition
  }

//  case class IndexedAccount(_id: ObjectId,
//                            account_number: Double,
//                            balance: Double,
//                            firstName: String,
//                            lastName: String,
//                            age: Double,
//                            gender: String,
//                            address: String,
//                            employer: String,
//                            email: String,
//                            city: String,
//                            state: String) extends Indexed("accounts") {
//
//    def mapping(): MappingDefinition =
//      MappingDefinition(indx)
//      .as(
//        BasicFieldDefinition("firstname", "Keyword")
//      )
//  }

}
