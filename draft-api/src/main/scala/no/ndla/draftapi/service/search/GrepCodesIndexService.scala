/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.domain.Article
import no.ndla.draftapi.model.search.{SearchableGrepCode, SearchableLanguageFormats}
import no.ndla.draftapi.repository.{DraftRepository, Repository}
import org.json4s.Formats
import org.json4s.native.Serialization.write

trait GrepCodesIndexService {
  this: SearchConverterService with IndexService with DraftRepository =>
  val grepCodesIndexService: GrepCodesIndexService

  class GrepCodesIndexService extends LazyLogging with IndexService[Article, SearchableGrepCode] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = DraftApiProperties.DraftGrepCodesSearchDocument
    override val searchIndex: String = DraftApiProperties.DraftGrepCodesSearchIndex
    override val repository: Repository[Article] = draftRepository

    override def createIndexRequests(domainModel: Article, indexName: String): Seq[IndexRequest] = {
      val grepCodes = searchConverterService.asSearchableGrepCodes(domainModel)

      grepCodes.map(code => {
        val source = write(code)
        indexInto(indexName / documentType).doc(source).id(code.grepCode)
      })
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          textField("grepCode")
        )
      )
    }
  }

}
