package interfaces

import interfaces.Indexable.{Id, ObjectId}
import org.json4s.CustomSerializer
import org.json4s.JsonAST._
import org.json4s.JsonDSL._

class ObjectIdSerializer extends CustomSerializer[ObjectId](format => ( {
  case jsonObj: JObject =>
    val ids = (jsonObj \ "ids").extract[List[Id]]
    ObjectId(ids)
}, {
  case obj: ObjectId =>
    "name" -> obj.ids.head.oid
}
))