package no.ndla.articleapi.model.api

import com.scalatsi.TypescriptType.TSNull
import com.scalatsi._

object TSTypes {
  implicit val nullAlias: TSNamedType[Null] =
    TSType.alias[Null]("NullAlias", TSNull) // https://github.com/scala-tsi/scala-tsi/issues/172

  // Type-aliases referencing generics doesn't not work without this in scala-tsi. See: https://github.com/scala-tsi/scala-tsi/issues/184
  implicit val relatedContent: TSIType[RelatedContentLink] = TSType.fromCaseClass[RelatedContentLink]
  // Scala2 enumerations doesn't work as expected in scala-tsi. See: https://github.com/scala-tsi/scala-tsi/issues/182
  implicit val availability: TSType[Availability.Value] = TSType.sameAs[Availability.Value, Availability.type]
}
