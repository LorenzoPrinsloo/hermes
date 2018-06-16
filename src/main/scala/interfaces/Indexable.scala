package interfaces

object Indexable {

  sealed trait ElasticIndexable

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
                     state: String
                    ) extends ElasticIndexable


}
