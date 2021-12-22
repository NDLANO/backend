package no.ndla.articleapi.model.api

import com.scalatsi._
import no.ndla.articleapi.model.domain

object TSTypes {
  // Type-aliases referencing generics doesn't not work without this in scala-tsi. See: https://github.com/scala-tsi/scala-tsi/issues/184
  implicit val relatedContent: TSIType[RelatedContentLink] = TSType.fromCaseClass[RelatedContentLink]
  // Scala2 enumerations doesn't work as expected in scala-tsi. See: https://github.com/scala-tsi/scala-tsi/issues/182
  implicit val availability: TSType[domain.Availability.Value] =
    TSType.sameAs[domain.Availability.Value, domain.Availability.type]
}
