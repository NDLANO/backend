/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import com.scalatsi.TypescriptType.TSNull
import com.scalatsi._
import no.ndla.common.model.api.RelatedContentLink
import no.ndla.common.model.domain.Availability

/** The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here. This is only necessary if the `sbt generateTypescript`
  * script fails.
  */
object TSTypes {
  // This alias is required since scala-tsi doesn't understand that Null is `null`
  // See: https://github.com/scala-tsi/scala-tsi/issues/172
  implicit val nullTsType: TSType[Null] = TSType(TSNull)

  // Scala2 enumerations doesn't work as expected in scala-tsi. See: https://github.com/scala-tsi/scala-tsi/issues/182
  implicit val availability: TSType[Availability.Value] = TSType.sameAs[Availability.Value, Availability.type]

  implicit val author              = TSType.fromCaseClass[Author]
  implicit val requiredLibrary     = TSType.fromCaseClass[RequiredLibrary]
  implicit val editorNote          = TSType.fromCaseClass[EditorNote]
  implicit val relatedContentLink  = TSType.fromCaseClass[RelatedContentLink]
  implicit val newArticleMetaImage = TSType.fromCaseClass[NewArticleMetaImage]
}
