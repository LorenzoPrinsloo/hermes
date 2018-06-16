package interfaces

object Serializable {

  sealed trait ElasticSerializable {
    def _id: String
  }

  case class SAccount(_id: String,
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
                      state: String) extends ElasticSerializable
}
