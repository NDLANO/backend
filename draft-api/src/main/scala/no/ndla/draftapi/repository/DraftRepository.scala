/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.common.model.domain.{ArticleType, EditorNote, Priority}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.model.api.{
  ArticleVersioningException,
  DraftErrorHelpers,
  GenerateIDException,
  NotFoundException
}
import no.ndla.draftapi.model.domain.*
import no.ndla.network.tapir.auth.TokenUser
import org.postgresql.util.PGobject
import scalikejdbc.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

class DraftRepository(using draftErrorHelpers: DraftErrorHelpers, clock: Clock)
    extends StrictLogging
    with Repository[Draft] {
  import draftErrorHelpers.*
  def insert(article: Draft)(implicit session: DBSession): Draft = {
    val startRevision = article.revision.getOrElse(1)
    val dataObject    = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(article))
    val slug = article.slug.map(_.toLowerCase)

    val dbId = sql"""
            insert into ${DBArticle.table} (document, revision, article_id, slug)
            values ($dataObject, $startRevision, ${article.id}, $slug)
          """.updateAndReturnGeneratedKey()

    logger.info(s"Inserted new article: ${article.id}, with revision $startRevision (with db id $dbId)")
    article.copy(revision = Some(startRevision), slug = slug)
  }

  def storeArticleAsNewVersion(article: Draft, user: Option[TokenUser], keepDraftData: Boolean = false)(implicit
      session: DBSession
  ): Try[Draft] = {
    article.id match {
      case None            => Failure(ArticleVersioningException("Duplication of article failed."))
      case Some(articleId) =>
        val correctRevision = withId(articleId).exists(_.revision.getOrElse(0) == article.revision.getOrElse(0))
        if (!correctRevision) {
          val message = s"Found revision mismatch when attempting to copy article ${article.id}"
          logger.info(message)
          Failure(new OptimisticLockException)
        } else {
          val externalIds: List[String]       = getExternalIdsFromId(articleId)
          val externalSubjectIds: Seq[String] = getExternalSubjectIdsFromId(articleId)
          val importId: Option[String]        = getImportIdFromId(articleId)
          val articleRevision                 = article.revision.getOrElse(0) + 1
          val copiedArticle                   = article.copy(
            notes = user
              .map(u => EditorNote("Artikkelen har blitt lagret som ny versjon", u.id, article.status, clock.now()))
              .toList,
            previousVersionsNotes = article.previousVersionsNotes ++ article.notes,
            responsible = if (keepDraftData) article.responsible else None,
            priority = if (keepDraftData) article.priority else Priority.Unspecified,
            comments =
              if (keepDraftData | article.articleType == ArticleType.TopicArticle) article.comments
              else Seq.empty
          )

          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(CirceUtil.toJsonString(copiedArticle))
          val uuid = Try(importId.map(UUID.fromString)).toOption.flatten
          val slug = article.slug.map(_.toLowerCase)

          val dbId: Long =
            sql"""
                 insert into ${DBArticle.table} (external_id, external_subject_id, document, revision, import_id, article_id, slug)
                 values (ARRAY[$externalIds]::text[],
                         ARRAY[$externalSubjectIds]::text[],
                         $dataObject,
                         $articleRevision,
                         $uuid,
                         $articleId,
                         $slug)
              """.updateAndReturnGeneratedKey()

          logger.info(s"Inserted new article: $articleId (with db id $dbId)")
          Success(copiedArticle.copy(revision = Some(articleRevision)))
        }
    }
  }

  def newEmptyArticleId()(implicit session: DBSession): Try[Long] = {
    Try(
      sql"SELECT NEXTVAL('article_id_sequence') as article_id"
        .map(rs => rs.long("article_id"))
        .single
        .apply()
    ) match {
      case Success(Some(articleId)) =>
        logger.info(s"Generated new article id: $articleId")
        Success(articleId)
      case Success(_) =>
        Failure(GenerateIDException("No id gotten when generating id in postgresql statement, this is weird."))
      case Failure(ex) => Failure(ex)
    }
  }

  private def failIfRevisionMismatch(count: Int, article: Draft, newRevision: Int): Try[Draft] =
    if (count != 1) {
      val message =
        s"Found revision mismatch when attempting to update article ${article.id.getOrElse(-1)} (Updated $count rows...)"
      logger.warn(message)
      Failure(new OptimisticLockException)
    } else {
      logger.info(s"Updated article ${article.id.getOrElse(-1)}")
      val updatedArticle = article.copy(revision = Some(newRevision))
      Success(updatedArticle)
    }

  def updateArticle(article: Draft)(implicit session: DBSession): Try[Draft] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(article))

    val newRevision = article.revision.getOrElse(0) + 1
    val oldRevision = article.revision.getOrElse(0)
    val slug        = article.slug.map(_.toLowerCase)
    val count       =
      sql"""
              update ${DBArticle.table}
              set document=$dataObject, revision=$newRevision, slug=$slug
              where article_id=${article.id}
              and revision=$oldRevision
              and revision=(select max(revision) from ${DBArticle.table} where article_id=${article.id})
           """.update()

    failIfRevisionMismatch(count, article, newRevision)
  }

  def withId(articleId: Long)(implicit session: DBSession): Option[Draft] =
    articleWhere(
      sqls"""
              ar.article_id=${articleId.toInt}
              ORDER BY revision
              DESC LIMIT 1
              """
    )

  def withIds(articleIds: List[Long], offset: Long, pageSize: Long)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): Try[Seq[Draft]] = Try {
    val ar  = DBArticle.syntax("ar")
    val ar2 = DBArticle.syntax("ar2")
    sql"""
        select ${ar.result.*}
        from ${DBArticle.as(ar)}
        where ar.document is not NULL
        and ar.article_id in ($articleIds)
        and ar.revision = (
            select max(revision)
            from ${DBArticle.as(ar2)}
            where ar2.article_id = ar.article_id
        )
        offset $offset
        limit $pageSize
         """
      .map(DBArticle.fromResultSet(ar))
      .list()
  }

  def idsWithStatus(status: DraftStatus)(implicit session: DBSession): Try[List[ArticleIds]] = {
    val ar = DBArticle.syntax("ar")
    Try(
      sql"select article_id, external_id from ${DBArticle
          .as(ar)} where ar.document is not NULL and ar.document#>>'{status,current}' = ${status.toString}"
        .map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs)))
        .list()
    )
  }

  def exists(id: Long)(implicit session: DBSession): Boolean = {
    sql"select article_id from ${DBArticle.table} where article_id=$id order by revision desc limit 1"
      .map(rs => rs.long("article_id"))
      .single()
      .isDefined
  }

  def deleteArticle(articleId: Long)(implicit session: DBSession): Try[Long] = {
    val numRows = sql"delete from ${DBArticle.table} where article_id = $articleId".update()
    if (numRows == 1) {
      Success(articleId)
    } else {
      Failure(NotFoundException(s"Article with id $articleId does not exist"))
    }
  }

  def deleteArticleRevision(articleId: Long, revision: Int)(implicit session: DBSession): Try[Unit] =
    Try(sql"delete from ${DBArticle.table} where article_id = $articleId and revision = $revision".update()).flatMap {
      case 1 => Success(())
      case _ => Failure(NotFoundException(s"Article with id $articleId and revision $revision does not exist"))
    }

  def getCurrentAndPreviousRevision(articleId: Long)(implicit session: DBSession): Try[(Draft, Draft)] = {
    val ar = DBArticle.syntax("ar")
    Try(
      sql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.article_id = $articleId order by revision desc limit 2"
        .map(DBArticle.fromResultSet(ar))
        .toList()
    ).flatMap {
      case List(current, previous) => Success((current, previous))
      case _ => Failure(NotFoundException(s"Article with id $articleId has fewer than 2 revisions"))
    }
  }

  def getIdFromExternalId(externalId: String)(implicit session: DBSession): Option[Long] = {
    sql"select article_id from ${DBArticle.table} where $externalId = any (external_id) order by revision desc limit 1"
      .map(rs => rs.long("article_id"))
      .single()
  }

  private def externalIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
    Option(wrappedResultSet.array("external_id"))
      .map(_.getArray.asInstanceOf[Array[String]])
      .getOrElse(Array.empty[String])
      .toList
      .flatMap(Option(_))
  }

  def getExternalIdsFromId(id: Long)(implicit session: DBSession): List[String] = {
    sql"select external_id from ${DBArticle.table} where article_id=${id.toInt} order by revision desc limit 1"
      .map(externalIdsFromResultSet)
      .single()
      .getOrElse(List.empty)
  }

  private def externalSubjectIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
    Option(wrappedResultSet.array("external_subject_id"))
      .map(_.getArray.asInstanceOf[Array[String]])
      .getOrElse(Array.empty[String])
      .toList
      .flatMap(Option(_))
  }

  def getExternalSubjectIdsFromId(id: Long)(implicit session: DBSession): Seq[String] = {
    sql"select external_subject_id from ${DBArticle.table} where article_id=${id.toInt} order by revision desc limit 1"
      .map(externalSubjectIdsFromResultSet)
      .single()
      .getOrElse(List.empty)
  }

  def getImportIdFromId(id: Long)(implicit session: DBSession): Option[String] = {
    sql"select import_id from ${DBArticle.table} where article_id=${id.toInt} order by revision desc limit 1"
      .map(rs => rs.string("import_id"))
      .single()
  }

  def getAllIds(implicit session: DBSession): Seq[ArticleIds] = {
    sql"select article_id, max(external_id) as external_id from ${DBArticle.table} group by article_id order by article_id asc"
      .map(rs =>
        ArticleIds(
          rs.long("article_id"),
          externalIdsFromResultSet(rs)
        )
      )
      .list()
  }

  def articleCount(implicit session: DBSession): Long = {
    sql"select count(distinct article_id) from ${DBArticle.table} where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def getArticlesByPage(pageSize: Int, offset: Int)(implicit session: DBSession): Seq[Draft] = {
    val ar = DBArticle.syntax("ar")
    sql"""
           select *
           from (select
                   ${ar.result.*},
                   ${ar.id} as row_id,
                   ${ar.revision} as revision,
                   max(revision) over (partition by article_id) as max_revision
                 from ${DBArticle.as(ar)}
                 where document is not NULL) _
           where revision = max_revision
           order by row_id
           offset $offset
           limit $pageSize
      """
      .map(DBArticle.fromResultSet(ar))
      .list()
  }

  def minMaxArticleId(implicit session: DBSession): (Long, Long) = {
    sql"select coalesce(MIN(article_id),0) as mi, coalesce(MAX(article_id),0) as ma from ${DBArticle.table}"
      .map(rs => {
        (rs.long("mi"), rs.long("ma"))
      })
      .single() match {
      case Some(minmax) => minmax
      case None         => (0L, 0L)
    }
  }

  override def minMaxId(implicit session: DBSession): (Long, Long) = {
    sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${DBArticle.table}"
      .map(rs => {
        (rs.long("mi"), rs.long("ma"))
      })
      .single() match {
      case Some(minmax) => minmax
      case None         => (0L, 0L)
    }
  }

  def documentsWithArticleIdBetween(min: Long, max: Long)(implicit session: DBSession): List[Draft] = {
    val ar       = DBArticle.syntax("ar")
    val subquery = DBArticle.syntax("b")
    sql"""
           select ${ar.result.*}
           from ${DBArticle.as(ar)}
           where ar.document is not NULL
           and ar.article_id between $min and $max
           and ar.document#>>'{status,current}' <> ${DraftStatus.ARCHIVED.toString}
           and ar.revision = (
             select max(b.revision)
             from ${DBArticle.as(subquery)}
             where b.article_id = ar.article_id
           )

           """
      .map(DBArticle.fromResultSet(ar))
      .list()
  }

  override def documentsWithIdBetween(min: Long, max: Long)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): List[Draft] = {
    val ar       = DBArticle.syntax("ar")
    val subquery = DBArticle.syntax("b")
    sql"""
           select ${ar.result.*}
           from ${DBArticle.as(ar)}
           where ar.document is not NULL
           and ar.id between $min and $max
           and ar.document#>>'{status,current}' <> ${DraftStatus.ARCHIVED.toString}
           and ar.revision = (
             select max(b.revision)
             from ${DBArticle.as(subquery)}
             where b.article_id = ar.article_id
           )

           """
      .map(DBArticle.fromResultSet(ar))
      .list()
  }

  private def articleWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession): Option[Draft] = {
    val ar = DBArticle.syntax("ar")

    sql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.document is not NULL and $whereClause "
      .map(DBArticle.fromResultSet(ar))
      .single()
  }

  def articlesWithId(articleId: Long): List[Draft] =
    articlesWhere(sqls"ar.article_id = $articleId").toList

  private def articlesWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Seq[Draft] = {
    val ar = DBArticle.syntax("ar")
    sql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.document is not NULL and $whereClause"
      .map(DBArticle.fromResultSet(ar))
      .list()
  }

  def importIdOfArticle(externalId: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[ImportId] = {
    val ar = DBArticle.syntax("ar")
    sql"""select ${ar.result.*}, import_id, external_id
            from ${DBArticle.as(ar)}
            where ar.document is not NULL and $externalId = any (ar.external_id)"""
      .map(rs => ImportId(rs.stringOpt("import_id")))
      .single()
  }

  def withSlug(slug: String)(implicit session: DBSession): Option[Draft] =
    articleWhere(sqls"ar.slug=${slug.toLowerCase} ORDER BY revision DESC LIMIT 1")

  def slugExists(slug: String, articleId: Option[Long])(implicit
      session: DBSession = ReadOnlyAutoSession
  ): Boolean = {
    val sq = articleId match {
      case None     => sql"select count(*) from ${DBArticle.table} where slug = ${slug.toLowerCase}"
      case Some(id) =>
        sql"select count(*) from ${DBArticle.table} where slug = ${slug.toLowerCase} and article_id != $id"
    }
    val count = sq.map(rs => rs.long("count")).single().getOrElse(0L)
    count > 0L
  }

}
