/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */


package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.{MappingContentBuilder, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.Index
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.domain.Article
import no.ndla.draftapi.model.domain.Language.languageAnalyzers
import no.ndla.draftapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.draftapi.repository.{DraftRepository, Repository}
import org.json4s.native.Serialization.write

trait ArticleIndexService {
  this: SearchConverterService with IndexService with DraftRepository =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article, SearchableArticle] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = DraftApiProperties.DraftSearchDocument
    override val searchIndex: String = DraftApiProperties.DraftSearchIndex
    override val repository: Repository[Article] = draftRepository

    override def createIndexRequest(domainModel: Article, indexName: String): Index = {
      val source = write(searchConverterService.asSearchableArticle(domainModel))
      new Index.Builder(source).index(indexName).`type`(documentType).id(domainModel.id.get.toString).build
    }

    def getMapping: String = {
      MappingContentBuilder.buildWithName(mapping(documentType).fields(
        intField("id"),
        languageSupportedField("title", keepRaw = true),
        languageSupportedField("content"),
        languageSupportedField("visualElement"),
        languageSupportedField("introduction"),
        languageSupportedField("tags"),
        dateField("lastUpdated"),
        keywordField("license") index "not_analyzed",
        textField("authors") fielddata true,
        textField("articleType") analyzer "keyword"
      ), DraftApiProperties.DraftSearchDocument).string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer fields (keywordField("raw") index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }

  }
}
