/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.domain.Agreement
import no.ndla.draftapi.model.search.SearchableArticle
import no.ndla.draftapi.repository.{AgreementRepository, Repository}
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.write

trait AgreementIndexService {
  this: SearchConverterService with IndexService with AgreementRepository with Props =>
  val agreementIndexService: AgreementIndexService

  class AgreementIndexService extends LazyLogging with IndexService[Agreement, SearchableArticle] {
    implicit val formats: Formats                  = SearchableLanguageFormats.JSonFormats
    override val documentType: String              = props.AgreementSearchDocument
    override val searchIndex: String               = props.AgreementSearchIndex
    override val repository: Repository[Agreement] = agreementRepository

    override def createIndexRequests(domainModel: Agreement, indexName: String): Seq[IndexRequest] = {
      val source = write(searchConverterService.asSearchableAgreement(domainModel))
      Seq(indexInto(indexName).doc(source).id(domainModel.id.get.toString))
    }

    def getMapping: MappingDefinition = {
      properties(
        intField("id"),
        textField("title").fielddata(false).fields(keywordField("raw")),
        textField("content").fielddata(false),
        keywordField("license")
      )
    }
  }
}
