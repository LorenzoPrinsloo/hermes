package interfaces

object Indexable {

  sealed trait ElasticIndexable

  case class Account(account_number: Int,
                     balance: BigDecimal,
                     firstName: String,
                     lastName: String,
                     age: Int,
                     gender: String,
                     address: String,
                     employer: String,
                     email: String,
                     city: String,
                     state: String
                    ) extends ElasticIndexable


}
