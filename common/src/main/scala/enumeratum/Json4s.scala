package enumeratum

import org.json4s.{CustomSerializer, Serializer}

object Json4s {
  // TODO: Either make a json formats thingy here or remove enumeratum completely?
  //       Scala 3 enums are appearently pretty good so maybe look into them?
  def serializer[A <: EnumEntry: Manifest](en: Enum[A]): CustomSerializer[A] = ???

}
