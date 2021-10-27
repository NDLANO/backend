package no.ndla.audioapi.model.api

import com.scalatsi.TypescriptType.TSNull
import com.scalatsi._

/**
  * The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here.
  * This is only necessary if the `sbt generateTypescript` script fails.
  */
object TSTypes {
  // This alias is required since scala-tsi doesn't understand that Null is `null`
  // See: https://github.com/scala-tsi/scala-tsi/issues/172
  //implicit val nullTsType: TSType[Null] = TSType(TSNull)

  implicit val author: TSIType[Author] = TSType.fromCaseClass[Author]
  // implicit val validationMessage: TSIType[ValidationMessage] = TSType.fromCaseClass[ValidationMessage]
}
