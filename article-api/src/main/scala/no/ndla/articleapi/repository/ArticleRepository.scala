/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain.{ArticleIds, ArticleRow, DBArticle}
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Article
import no.ndla.database.DataSource
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

trait ArticleRepository {
  this: DataSource & DBArticle =>
  val articleRepository: ArticleRepository

  class ArticleRepository extends StrictLogging {
    def updateArticleFromDraftApi(article: Article, externalIds: List[String])(implicit
        session: DBSession = AutoSession
    ): Try[Article] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      val jsonString = CirceUtil.toJsonString(article)
      dataObject.setValue(jsonString)
      val slug = article.slug.map(_.toLowerCase)

      Try {
        sql"""update ${Article.table}
              set document=$dataObject,
                  external_id=ARRAY[$externalIds]::text[],
                  slug=$slug
              where article_id=${article.id} and revision=${article.revision}
          """.update()
      } match {
        case Success(count) if count == 1 =>
          logger.info(s"Updated article ${article.id}")
          Success(article.copy(slug = slug))
        case Success(_) =>
          logger.info(s"No article with id ${article.id} and revision ${article.revision} exists, creating...")
          val slug = article.slug.map(_.toLowerCase)
          Try {
            sql"""
                  insert into ${Article.table} (article_id, document, external_id, revision, slug)
                  values (${article.id}, $dataObject, ARRAY[$externalIds]::text[], ${article.revision}, $slug)
              """.updateAndReturnGeneratedKey()
          }.map(_ => article.copy(slug = slug))

        case Failure(ex) => Failure(ex)
      }
    }

    def unpublishMaxRevision(articleId: Long)(implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows =
        sql"""
             update ${Article.table}
             set document=null
             where article_id=$articleId
             and revision=(select max(revision) from ${Article.table} where article_id=$articleId)
           """.update()
      if (numRows == 1) {
        Success(articleId)
      } else {
        Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
    }

    def unpublish(articleId: Long, revision: Int)(implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows =
        sql"""
             update ${Article.table}
             set document=null, slug=null
             where article_id=$articleId
             and revision=$revision
           """.update()
      if (numRows == 1) {
        Success(articleId)
      } else {
        Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
    }

    def deleteMaxRevision(articleId: Long)(implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows =
        sql"""
             delete from ${Article.table}
             where article_id = $articleId
             and revision=(select max(revision) from ${Article.table} where article_id=$articleId)
           """.update()
      if (numRows == 1) {
        Success(articleId)
      } else {
        // Article with id $articleId does not exist.
        Success(articleId)
      }
    }

    def delete(articleId: Long, revision: Int)(implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows =
        sql"""
             delete from ${Article.table}
             where article_id = $articleId
             and revision=$revision
           """.update()
      if (numRows == 1) {
        Success(articleId)
      } else {
        // Article with id $articleId does not exist.
        Success(articleId)
      }
    }

    def withSlug(slug: String): Option[ArticleRow] = articleWhere(
      sqls"ar.slug=${slug.toLowerCase} ORDER BY revision DESC LIMIT 1"
    )

    def withId(articleId: Long): Option[ArticleRow] =
      articleWhere(
        sqls"""
              ar.article_id=${articleId.toInt}
              ORDER BY revision DESC
              LIMIT 1
              """
      )

    def withIdAndRevision(articleId: Long, revision: Int): Option[ArticleRow] = {
      articleWhere(
        sqls"""
              ar.article_id=${articleId.toInt}
              and ar.revision=$revision
              """
      )
    }

    def withIds(articleIds: List[Long], offset: Int, pageSize: Int)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Seq[ArticleRow] = {
      val ar  = Article.syntax("ar")
      val ar2 = Article.syntax("ar2")
      sql"""
        select ${ar.result.*}
        from ${Article.as(ar)}
        where ar.document is not NULL
        and ar.article_id in ($articleIds)
        and ar.revision = (
            select max(revision)
            from ${Article.as(ar2)}
            where ar2.article_id = ar.article_id
        )
        offset $offset
        limit $pageSize
         """
        .map(Article.fromResultSet(ar))
        .list()
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"""
           select article_id
           from ${Article.table}
           where $externalId = any(external_id)
           order by revision desc
           limit 1
         """
        .map(rs => rs.long("article_id"))
        .single()
    }

    def getRevisions(articleId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Int] = {
      sql"""
            select revision
            from ${Article.table}
            where article_id=$articleId
            and document is not NULL;
         """
        .map(rs => rs.int("revision"))
        .list()
    }

    private def externalIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
      Option(wrappedResultSet.array("external_id"))
        .map(_.getArray.asInstanceOf[Array[String]])
        .getOrElse(Array.empty)
        .toList
    }

    def getExternalIdsFromId(id: Long)(implicit session: DBSession = AutoSession): List[String] = {
      sql"""
           select external_id
           from ${Article.table}
           where article_id=${id.toInt}
           order by revision desc
           limit 1
         """
        .map(externalIdsFromResultSet)
        .single()
        .getOrElse(List.empty)
    }

    def getAllIds(implicit session: DBSession = AutoSession): Seq[ArticleIds] = {
      sql"select article_id, external_id from ${Article.table}"
        .map(rs =>
          ArticleIds(
            rs.long("article_id"),
            externalIdsFromResultSet(rs)
          )
        )
        .list()
    }

    def articleCount(implicit session: DBSession = AutoSession): Long = {
      val ar = Article.syntax("ar")
      sql"""
           select count(distinct article_id)
           from (select
                   *,
                   ar.document as doc,
                   max(revision) over (partition by article_id) as max_revision
                 from ${Article.as(ar)}
                 ) _
           where revision = max_revision
           and doc is not null
      """
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0)
    }

    def getArticlesByPage(pageSize: Int, offset: Int)(implicit session: DBSession = AutoSession): Seq[Article] = {
      val ar = Article.syntax("ar")
      sql"""
           select *
           from (select
                   ${ar.result.*},
                   ${ar.revision} as revision,
                   ${ar.id} as row_id,
                   ar.document as doc,
                   max(revision) over (partition by article_id) as max_revision
                 from ${Article.as(ar)}) _
           where revision = max_revision
           and doc is not null
           order by row_id
           offset $offset
           limit $pageSize
      """
        .map(Article.fromResultSet(ar))
        .list()
        .toArticles
    }

    def getTags(input: String, pageSize: Int, offset: Int, language: String)(implicit
        session: DBSession = AutoSession
    ): (Seq[String], Long) = {
      val sanitizedInput    = input.replaceAll("%", "")
      val sanitizedLanguage = language.replaceAll("%", "")
      val langOrAll         = if (sanitizedLanguage == "*" || sanitizedLanguage == "") "%" else sanitizedLanguage

      val tags = sql"""select tags from
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${Article.table}) _
              where tagObj->>'language' like $langOrAll
              order by tags) sorted_tags
              where sorted_tags.tags ilike ${sanitizedInput + '%'}
              offset $offset
              limit $pageSize
                      """
        .map(rs => rs.string("tags"))
        .list()

      val tagsCount =
        sql"""
              select count(*) from
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${Article.table}) _
              where tagObj->>'language' like  $langOrAll) all_tags
              where all_tags.tags ilike ${sanitizedInput + '%'};
           """
          .map(rs => rs.int("count"))
          .single()
          .getOrElse(0)

      (tags, tagsCount.toLong)

    }

    def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(article_id),0) as mi, coalesce(MAX(article_id),0) as ma from ${Article.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    def documentsWithIdBetween(min: Long, max: Long)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Seq[Article] = {
      val article         = Article.syntax("a")
      val subqueryArticle = Article.syntax("b")

      sql"""
        select ${article.result.*}
        from ${Article.as(article)}
        where a.document is not NULL
        and a.article_id between $min and $max
        and a.revision = (
            select max(b.revision)
            from ${Article.as(subqueryArticle)}
            where b.article_id = a.article_id
          )
         """
        .map(Article.fromResultSet(article))
        .list()
        .toArticles
    }

    private def articleWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Option[ArticleRow] = {
      val ar = Article.syntax("ar")
      sql"""
           select ${ar.result.*}
           from ${Article.as(ar)}
           where $whereClause
         """
        .map(Article.fromResultSet(ar))
        .single()
    }

    def getArticleIdsFromExternalId(
        externalId: String
    )(implicit session: DBSession = ReadOnlyAutoSession): Option[ArticleIds] = {
      val ar = Article.syntax("ar")
      sql"""select distinct article_id, external_id
        from ${Article.as(ar)}
        where $externalId=ANY(ar.external_id)
        and ar.document is not NULL"""
        .map(rs =>
          ArticleIds(
            rs.long("article_id"),
            externalIdsFromResultSet(rs)
          )
        )
        .single()
    }

    def slugExists(slug: String, articleId: Option[Long])(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Boolean = {
      val sq = articleId match {
        case None => sql"select count(*) from ${Article.table} where slug = ${slug.toLowerCase}"
        case Some(id) =>
          sql"select count(*) from ${Article.table} where slug = ${slug.toLowerCase} and article_id != $id"
      }
      val count = sq.map(rs => rs.long("count")).single().getOrElse(0L)
      count > 0L
    }

  }

}
