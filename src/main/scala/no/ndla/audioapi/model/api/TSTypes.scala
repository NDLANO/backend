package no.ndla.audioapi.model.api

import com.scalatsi._
import com.scalatsi.dsl._

/**
  * The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here.
  * This is only necessary if the `sbt generateTypescript` script fails.
  */
object TSTypes {
//  implicit val author: TSIType[Author] = TSType.fromCaseClass[Author]
}
