/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import com.scalatsi.TypescriptType.TSNull
import com.scalatsi._
import no.ndla.common.model.api.{AuthorDTO, RelatedContentLinkDTO}

/** The `scala-tsi` plugin is not always able to derive the types that are used in `Seq` or other generic types.
  * Therefore we need to explicitly load the case classes here. This is only necessary if the `sbt generateTypescript`
  * script fails.
  */
object TSTypes {
  // This alias is required since scala-tsi doesn't understand that Null is `null`
  // See: https://github.com/scala-tsi/scala-tsi/issues/172
  implicit val nullTsType: TSType[Null] = TSType(TSNull)

  implicit val author: TSIType[AuthorDTO]                           = TSType.fromCaseClass[AuthorDTO]
  implicit val requiredLibrary: TSIType[RequiredLibraryDTO]         = TSType.fromCaseClass[RequiredLibraryDTO]
  implicit val editorNote: TSIType[EditorNoteDTO]                   = TSType.fromCaseClass[EditorNoteDTO]
  implicit val relatedContentLink: TSIType[RelatedContentLinkDTO]   = TSType.fromCaseClass[RelatedContentLinkDTO]
  implicit val newArticleMetaImage: TSIType[NewArticleMetaImageDTO] = TSType.fromCaseClass[NewArticleMetaImageDTO]
}
