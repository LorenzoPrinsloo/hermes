package interfaces

object Indexable {

  sealed trait ElasticIndexable {
    def _id: ObjectId
  }

  case class IAccount(_id: ObjectId,
                      account_number: Double,
                      balance: Double,
                      firstName: String,
                      lastName: String,
                      age: Double,
                      gender: String,
                      address: String,
                      employer: String,
                      email: String,
                      city: String,
                      state: String
                    ) extends ElasticIndexable

  case class ObjectId(ids: Seq[Id])
  case class Id(oid: String)
}
