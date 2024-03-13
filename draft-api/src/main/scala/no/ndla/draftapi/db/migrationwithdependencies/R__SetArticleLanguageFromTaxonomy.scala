/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import enumeratum.Json4s
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  ArticleType,
  Description,
  Introduction,
  Tag,
  Title,
  VisualElement
}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.{DraftApiProperties, Props}
import no.ndla.language.Language.languageOrUnknown
import no.ndla.mapping.ISO639.get6391CodeFor6392Code
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.*
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class R__SetArticleLanguageFromTaxonomy(properties: DraftApiProperties)
    extends BaseJavaMigration
    with DBArticle
    with Props {
  override val props: DraftApiProperties = properties

  implicit val formats: Formats = org.json4s.DefaultFormats +
    Json4s.serializer(DraftStatus) +
    Json4s.serializer(ArticleType)
  private val TaxonomyApiEndpoint = s"${props.Domain}/taxonomy/v1"
  private val taxonomyTimeout     = 20.seconds

  case class TaxonomyResource(contentUri: Option[String], id: Option[String])

  case class Keywords(keyword: List[Keyword])
  case class Keyword(names: List[KeywordName])
  case class KeywordName(data: List[Map[String, String]])

  override def getChecksum: Integer = 1 // Change this to something else if you want to repeat migration
  override def migrate(context: Context): Unit = {
    /*
    // Environments are migrated already
    // So this is a noop migration to speed up tests and fresh local runs
    // If we want to repeat migration just remove this comment and change checksum

    DB(context.getConnection)
      .autoClose(false)
      .withinTx { implicit session =>
        migrateArticles
      }
     */
  }

  def fetchResourceFromTaxonomy(endpoint: String): Seq[(Long, Option[Long])] = {
    val url = TaxonomyApiEndpoint + endpoint

    val resourceList = for {
      response  <- Try(simpleHttpClient.send(quickRequest.get(uri"$url").readTimeout(taxonomyTimeout)))
      extracted <- Try(parse(response.body).extract[Seq[TaxonomyResource]])
    } yield extracted

    resourceList.getOrElse(Seq.empty).flatMap(trim)
  }

  def trim(resource: TaxonomyResource): Option[(Long, Option[Long])] = {

    val convertedArticleId = resource.contentUri
      .filter(_.matches("urn\\:article\\:[0-9]*"))
      .flatMap(cu => Try(cu.split(':').last.toLong).toOption)

    val externalId = resource.id.flatMap(i => Try(i.split(':').last.toLong).toOption)

    convertedArticleId match {
      case Some(articleId) => Some((articleId, externalId))
      case _               => None
    }

  }

  def fetchArticleTags(externalId: Long): Seq[Tag] = {

    val url = "http://api.topic.ndla.no/rest/v1/keywords/?filter%5Bnode%5D=ndlanode_" + externalId.toString

    val keywordsT = for {
      response  <- Try(simpleHttpClient.send(quickRequest.get(uri"$url")))
      extracted <- Try(parse(response.body).extract[Keywords])
    } yield extracted

    keywordsT match {
      case Failure(_) => Seq()
      case Success(keywords) =>
        keywords.keyword
          .flatMap(_.names)
          .flatMap(_.data)
          .flatMap(_.toList)
          .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
          .groupBy(_._1)
          .map(entry => (entry._1, entry._2.map(_._2)))
          .map(t => Tag(t._2, languageOrUnknown(t._1).toString))
          .toList
    }

  }

  def getISO639(languageUrl: String): Option[String] = {
    val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")
    Option(languageUrl) collect { case pattern(group) => group } match {
      case Some(x) =>
        if (x == "language-neutral") None else get6391CodeFor6392Code(x)
      case None => None
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {

    val topicIdsList: Seq[(Long, Option[Long])] = fetchResourceFromTaxonomy(
      "/subjects/urn:subject:15/topics?recursive=true"
    )
    val convertedTopicArticles = topicIdsList.map(topicIds => convertArticle(topicIds._1, topicIds._2))

    (for {
      convertedArticle <- convertedTopicArticles
      article          <- convertedArticle
    } yield updateArticle(article)): Unit

    val resourceIdsList: Seq[(Long, Option[Long])] = fetchResourceFromTaxonomy("/subjects/urn:subject:15/resources")
    val convertedResourceArticles = resourceIdsList.map(topicIds => convertArticle(topicIds._1, topicIds._2))

    (for {
      convertedArticle <- convertedResourceArticles
      article          <- convertedArticle
    } yield updateArticle(article)): Unit

  }

  def convertArticle(articleId: Long, externalId: Option[Long])(implicit session: DBSession): Option[Draft] = {
    val externalTags = externalId.map(fetchArticleTags).getOrElse(Seq())
    val oldArticle   = fetchArticleInfo(articleId)
    convertArticleLanguage(oldArticle, externalTags)
  }

  def fetchArticleInfo(articleId: Long)(implicit session: DBSession): Option[Draft] = {
    val ar = DBArticle.syntax("ar")
    val withId =
      sqls"ar.id=${articleId.toInt} ORDER BY revision DESC LIMIT 1"
    sql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.document is not NULL and $withId"
      .map(DBArticle.fromResultSet(ar))
      .single()
  }

  def convertArticleLanguage(oldArticle: Option[Draft], externalTags: Seq[Tag]): Option[Draft] = {
    val contentLanguages = oldArticle.map(_.content).getOrElse(Seq()).map(content => content.language)
    oldArticle.map(article =>
      article.copy(
        title = article.title.map(copyArticleTitle),
        content = article.content.map(copyArticleContent),
        tags = mergeTags(article.tags, externalTags, contentLanguages),
        visualElement = article.visualElement.map(copyVisualElement),
        introduction = article.introduction.map(copyArticleIntroduction),
        metaDescription = article.metaDescription.map(copyArticleMetaDescription),
        metaImage = article.metaImage.map(copyArticleMetaImage)
      )
    )
  }

  def mergeTags(
      oldTags: Seq[Tag],
      externalTags: Seq[Tag],
      contentLanguages: Seq[String]
  ): Seq[Tag] = {
    val combinedSeq = oldTags ++ externalTags
    combinedSeq
      .groupBy(_.language)
      .filter(mapEntry => contentLanguages.contains(mapEntry._1))
      .map(mapEntry => createTag(mapEntry._1, mapEntry._2))
      .toSeq
  }

  def createTag(language: String, tags: Seq[Tag]): Tag = {
    val distinctTags = tags.flatMap(_.tags).distinct
    Tag(distinctTags, language)
  }

  def copyArticleTitle(field: Title): Title = {
    if (field.language == "unknown") field.copy(language = "sma") else field
  }

  def copyArticleContent(field: ArticleContent): ArticleContent = {
    if (field.language == "unknown") field.copy(language = "sma") else field
  }

  def copyVisualElement(field: VisualElement): VisualElement = {
    if (field.language == "unknown") field.copy(language = "sma") else field
  }

  def copyArticleIntroduction(field: Introduction): Introduction = {
    if (field.language == "unknown") field.copy(language = "sma") else field
  }

  def copyArticleMetaDescription(field: Description): Description = {
    if (field.language == "unknown") field.copy(language = "sma") else field
  }

  def copyArticleMetaImage(field: ArticleMetaImage): ArticleMetaImage = {
    if (field.language == "unknown") field.copy(language = "sma") else field
  }

  def updateArticle(article: Draft)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(article))

    sql"update articledata set document = $dataObject where article_id=${article.id}"
      .update()
  }

}
