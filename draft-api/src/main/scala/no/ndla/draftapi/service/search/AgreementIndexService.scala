/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.domain.Agreement
import no.ndla.draftapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.draftapi.repository.{AgreementRepository, Repository}
import org.json4s.native.Serialization.write

trait AgreementIndexService {
  this: SearchConverterService with IndexService with AgreementRepository =>
  val agreementIndexService: AgreementIndexService

  class AgreementIndexService extends LazyLogging with IndexService[Agreement, SearchableArticle] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = DraftApiProperties.AgreementSearchDocument
    override val searchIndex: String = DraftApiProperties.AgreementSearchIndex
    override val repository: Repository[Agreement] = agreementRepository

    override def createIndexRequests(domainModel: Agreement, indexName: String): Seq[IndexRequest] = {
      val source = write(searchConverterService.asSearchableAgreement(domainModel))
      Seq(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        intField("id"),
        textField("title").fielddata(false).fields(keywordField("raw")),
        textField("content").fielddata(false),
        keywordField("license")
      )
    }
  }
}
