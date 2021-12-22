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
import no.ndla.draftapi.model.domain.Article
import no.ndla.draftapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.draftapi.repository.{DraftRepository, Repository}
import org.json4s.Formats
import org.json4s.native.Serialization.write

trait ArticleIndexService {
  this: SearchConverterService with IndexService with DraftRepository =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article, SearchableArticle] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = DraftApiProperties.DraftSearchDocument
    override val searchIndex: String = DraftApiProperties.DraftSearchIndex
    override val repository: Repository[Article] = draftRepository

    override def createIndexRequests(domainModel: Article, indexName: String): Seq[IndexRequest] = {
      val source = write(searchConverterService.asSearchableArticle(domainModel))
      Seq(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        intField("id"),
        dateField("lastUpdated"),
        keywordField("license"),
        keywordField("defaultTitle"),
        textField("authors") fielddata true,
        textField("articleType") analyzer "keyword",
        textField("notes"),
        textField("previousNotes"),
        keywordField("users"),
        keywordField("grepCodes"),
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("visualElement") ++
        generateLanguageSupportedDynamicTemplates("introduction") ++
        generateLanguageSupportedDynamicTemplates("tags")

      mapping(documentType).fields(fields).dynamicTemplates(dynamics)
    }
  }

}
