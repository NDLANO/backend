/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.model.NotFoundException
import no.ndla.articleapi.model.domain.{ArticleIds, ArticleRow, DBArticle}
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Article
import no.ndla.database.implicits.*
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

class ArticleRepository(using dbArticle: DBArticle) extends StrictLogging {

  def updateArticleFromDraftApi(article: Article, externalIds: List[String])(implicit
      session: DBSession
  ): Try[Article] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    val jsonString = CirceUtil.toJsonString(article)
    dataObject.setValue(jsonString)
    val slug = article.slug.map(_.toLowerCase)

    tsql"""
      update ${dbArticle.Article.table}
      set document=$dataObject,
        external_id=ARRAY[$externalIds]::text[],
        slug=$slug
      where article_id=${article.id} and revision=${article.revision}
    """.update() match {
      case Success(count) if count == 1 =>
        logger.info(s"Updated article ${article.id}")
        Success(article.copy(slug = slug))
      case Success(_) =>
        logger.info(s"No article with id ${article.id} and revision ${article.revision} exists, creating...")
        val slug = article.slug.map(_.toLowerCase)

        tsql"""
          insert into ${dbArticle.Article.table} (article_id, document, external_id, revision, slug)
          values (${article.id}, $dataObject, ARRAY[$externalIds]::text[], ${article.revision}, $slug)
        """.updateAndReturnGeneratedKey().map(_ => article.copy(slug = slug))

      case Failure(ex) => Failure(ex)
    }
  }

  def unpublishMaxRevision(articleId: Long)(using DBSession): Try[Long] = tsql"""
    update ${dbArticle.Article.table}
    set document=null
    where article_id=$articleId
    and revision=(select max(revision) from ${dbArticle.Article.table} where article_id=$articleId)
  """.update() match {
    case Success(count) if count == 1 => Success(articleId)
    case Success(_)                   => Failure(NotFoundException(s"Article with id $articleId does not exist"))
    case Failure(ex)                  => Failure(ex)
  }

  def unpublish(articleId: Long, revision: Int)(using DBSession): Try[Long] = tsql"""
    update ${dbArticle.Article.table}
    set document=null, slug=null
    where article_id=$articleId
    and revision=$revision
  """.update() match {
    case Success(count) if count == 1 => Success(articleId)
    case Success(_)                   => Failure(NotFoundException(s"Article with id $articleId does not exist"))
    case Failure(ex)                  => Failure(ex)
  }

  def deleteMaxRevision(articleId: Long)(using DBSession): Try[Long] = tsql"""
    delete from ${dbArticle.Article.table}
    where article_id = $articleId
    and revision=(select max(revision) from ${dbArticle.Article.table} where article_id=$articleId)
  """.update() match {
    case Success(_)  => Success(articleId)
    case Failure(ex) => Failure(ex)
  }

  def delete(articleId: Long, revision: Int)(using DBSession): Try[Long] = tsql"""
    delete from ${dbArticle.Article.table}
    where article_id = $articleId
    and revision=$revision
  """.update() match {
    case Success(_)  => Success(articleId)
    case Failure(ex) => Failure(ex)
  }

  def withSlug(slug: String)(using DBSession): Try[Option[ArticleRow]] =
    articleWhere(sqls"ar.slug=${slug.toLowerCase} ORDER BY revision DESC LIMIT 1")

  def withId(articleId: Long)(using DBSession): Try[Option[ArticleRow]] = articleWhere(sqls"""
    ar.article_id=${articleId.toInt}
    ORDER BY revision DESC
    LIMIT 1
  """)

  def withIdAndRevision(articleId: Long, revision: Int)(using DBSession): Try[Option[ArticleRow]] = articleWhere(sqls"""
    ar.article_id=${articleId.toInt}
    and ar.revision=$revision
  """)

  def withIds(articleIds: List[Long], offset: Int, pageSize: Int)(using DBSession): Try[Seq[ArticleRow]] = {
    val ar  = dbArticle.Article.syntax("ar")
    val ar2 = dbArticle.Article.syntax("ar2")
    tsql"""
      select ${ar.result.*}
      from ${dbArticle.Article.as(ar)}
      where ar.document is not NULL
      and ar.article_id in ($articleIds)
      and ar.revision = (
        select max(revision)
        from ${dbArticle.Article.as(ar2)}
        where ar2.article_id = ar.article_id
      )
      offset $offset
      limit $pageSize
    """.map(dbArticle.Article.fromResultSet(ar)).runList()
  }

  def getIdFromExternalId(externalId: String)(using DBSession): Try[Long] = {
    tsql"""
      select article_id
      from ${dbArticle.Article.table}
      where $externalId = any(external_id)
      order by revision desc
      limit 1
    """
      .map(rs => rs.long("article_id"))
      .runSingleTry(NotFoundException(s"Article with externalId $externalId not found"))
  }

  def getRevisions(articleId: Long)(using DBSession): Try[Seq[Int]] = {
    tsql"""
      select revision
      from ${dbArticle.Article.table}
      where article_id=$articleId
      and document is not NULL;
    """.map(rs => rs.int("revision")).runList()
  }

  private def externalIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
    Option(wrappedResultSet.array("external_id"))
      .map(x => x.getArray.asInstanceOf[Array[String]])
      .getOrElse(Array.empty[String])
      .toList
  }

  def getExternalIdsFromId(id: Long)(using DBSession): Try[List[String]] = {
    tsql"""
      select external_id
      from ${dbArticle.Article.table}
      where article_id=${id.toInt}
      order by revision desc
      limit 1
    """.map(externalIdsFromResultSet).runSingle().map(_.getOrElse(List.empty))
  }

  def getAllIds(using DBSession): Try[Seq[ArticleIds]] = {
    tsql"select article_id, external_id from ${dbArticle.Article.table}"
      .map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs)))
      .runList()
  }

  def articleCount(using DBSession): Try[Long] = {
    val ar = dbArticle.Article.syntax("ar")
    tsql"""
      select count(distinct article_id)
      from (
        select *, ar.document as doc, max(revision) over (partition by article_id) as max_revision
        from ${dbArticle.Article.as(ar)}
      ) _
      where revision = max_revision
      and doc is not null
    """.map(rs => rs.long("count")).runSingle().map(_.getOrElse(0))
  }

  def getArticlesByPage(pageSize: Int, offset: Int)(using DBSession): Try[Seq[Article]] = {
    val ar = dbArticle.Article.syntax("ar")
    tsql"""
      select *
      from (select
              ${ar.result.*},
              ${ar.revision} as revision,
              ${ar.id} as row_id,
              ar.document as doc,
              max(revision) over (partition by article_id) as max_revision
            from ${dbArticle.Article.as(ar)}) _
      where revision = max_revision
      and doc is not null
      order by row_id
      offset $offset
      limit $pageSize
    """.map(dbArticle.Article.fromResultSet(ar)).runList().map(_.toArticles)
  }

  def getTags(input: String, pageSize: Int, offset: Int, language: String)(implicit
      session: DBSession
  ): Try[(Seq[String], Long)] = {
    val sanitizedInput    = input.replaceAll("%", "")
    val sanitizedLanguage = language.replaceAll("%", "")
    val langOrAll         =
      if (sanitizedLanguage == "*" || sanitizedLanguage == "") "%"
      else sanitizedLanguage

    val triedTags = tsql"""select tags from
      (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
      (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${dbArticle.Article.table}) _
      where tagObj->>'language' like $langOrAll
      order by tags) sorted_tags
      where sorted_tags.tags ilike ${sanitizedInput + '%'}
      offset $offset
      limit $pageSize
    """.map(rs => rs.string("tags")).runList()

    val triedTagsCount = tsql"""
      select count(*) from
      (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
      (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${dbArticle.Article.table}) _
      where tagObj->>'language' like  $langOrAll) all_tags
      where all_tags.tags ilike ${sanitizedInput + '%'};
    """.map(rs => rs.int("count")).runSingle()

    for {
      tags           <- triedTags
      maybeTagsCount <- triedTagsCount
    } yield (tags, maybeTagsCount.getOrElse(0).toLong)
  }

  def minMaxId(using DBSession): Try[(Long, Long)] = {
    tsql"select coalesce(MIN(article_id),0) as mi, coalesce(MAX(article_id),0) as ma from ${dbArticle.Article.table}"
      .map(rs => {
        (rs.long("mi"), rs.long("ma"))
      })
      .runSingle() match {
      case Success(Some(minmax)) => Success(minmax)
      case Success(None)         => Success((0L, 0L))
      case Failure(ex)           => Failure(ex)
    }
  }

  def documentsWithIdBetween(min: Long, max: Long)(using DBSession): Try[Seq[Article]] = {
    val article         = dbArticle.Article.syntax("a")
    val subqueryArticle = dbArticle.Article.syntax("b")

    tsql"""
      select ${article.result.*}
      from ${dbArticle.Article.as(article)}
      where a.document is not NULL
      and a.article_id between $min and $max
      and a.revision = (
          select max(b.revision)
          from ${dbArticle.Article.as(subqueryArticle)}
          where b.article_id = a.article_id
        )
    """.map(dbArticle.Article.fromResultSet(article)).runList().map(_.toArticles)
  }

  private def articleWhere(whereClause: SQLSyntax)(using DBSession): Try[Option[ArticleRow]] = {
    val ar = dbArticle.Article.syntax("ar")
    tsql"""
      select ${ar.result.*}
      from ${dbArticle.Article.as(ar)}
      where $whereClause
    """.map(dbArticle.Article.fromResultSet(ar)).runSingle()
  }

  def getArticleIdsFromExternalId(externalId: String)(using DBSession): Try[Option[ArticleIds]] = {
    val ar = dbArticle.Article.syntax("ar")
    tsql"""
      select distinct article_id, external_id
      from ${dbArticle.Article.as(ar)}
      where $externalId=ANY(ar.external_id)
      and ar.document is not NULL
    """.map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs))).runSingle()
  }

  def slugExists(slug: String, articleId: Option[Long])(using DBSession): Try[Boolean] = {
    val existingSlugCountSql = articleId match {
      case None     => tsql"select count(*) from ${dbArticle.Article.table} where slug = ${slug.toLowerCase}"
      case Some(id) =>
        tsql"select count(*) from ${dbArticle.Article.table} where slug = ${slug.toLowerCase} and article_id != $id"
    }

    existingSlugCountSql.map(rs => rs.long("count")).runSingle().map(_.getOrElse(0L) > 0L)
  }
}
