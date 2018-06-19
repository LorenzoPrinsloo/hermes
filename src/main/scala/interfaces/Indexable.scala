package interfaces

import java.time.OffsetDateTime

import com.fullfacing.apollo.core.protocol.DomainModel
import com.sksamuel.elastic4s.mappings.MappingDefinition
import org.json4s.jackson.Serialization.write
import com.fullfacing.common.tcs.serializers.SerializationFormats.formats

object Indexable {

  case class Account(account_number: Double,
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
                    ) extends ElasticDomainModel

  sealed trait ElasticDomainModel extends DomainModel {
    def mapping(indx: String): MappingDefinition = MappingDefinition(indx, rawSource = Some(write(this)))
  }

}
