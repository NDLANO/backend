/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.articleapi.model.api

import com.scalatsi.TypescriptType.TSNull
import com.scalatsi.*
import no.ndla.common.model.api.RelatedContentLinkDTO

object TSTypes {
  implicit val nullAlias: TSNamedType[Null] =
    TSType.alias[Null]("NullAlias", TSNull) // https://github.com/scala-tsi/scala-tsi/issues/172

  // Type-aliases referencing generics does not work without this in scala-tsi. See: https://github.com/scala-tsi/scala-tsi/issues/184
  implicit val relatedContent: TSIType[RelatedContentLinkDTO] = TSType.fromCaseClass[RelatedContentLinkDTO]
}
