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
import no.ndla.database.implicits.*
import no.ndla.draftapi.model.api.{
  ArticleVersioningException,
  DraftErrorHelpers,
  GenerateIDException,
  NotFoundException,
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

  def insert(article: Draft)(using session: DBSession): Try[Draft] = {
    val startRevision = article.revision.getOrElse(1)
    val dataObject    = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(article))
    val slug = article.slug.map(_.toLowerCase)

    tsql"""
      insert into ${DBArticle.table} (document, revision, article_id, slug)
      values ($dataObject, $startRevision, ${article.id}, $slug)
    """
      .updateAndReturnGeneratedKey()
      .map { dbId =>
        logger.info(s"Inserted new article: ${article.id}, with revision $startRevision (with db id $dbId)")
        article.copy(revision = Some(startRevision), slug = slug)
      }
  }

  def storeArticleAsNewVersion(article: Draft, user: Option[TokenUser], keepDraftData: Boolean = false)(using
      session: DBSession
  ): Try[Draft] = {
    article.id match {
      case None            => Failure(ArticleVersioningException("Duplication of article failed."))
      case Some(articleId) => for {
          maybeCurrent <- withId(articleId)
          _            <- maybeCurrent match {
            case Some(current) if current.revision.getOrElse(0) == article.revision.getOrElse(0) => Success(())
            case _                                                                               =>
              val message = s"Found revision mismatch when attempting to copy article ${article.id}"
              logger.info(message)
              Failure(new OptimisticLockException)
          }
          externalIds        <- getExternalIdsFromId(articleId)
          externalSubjectIds <- getExternalSubjectIdsFromId(articleId)
          importId           <- getImportIdFromId(articleId)
          articleRevision     = article.revision.getOrElse(0) + 1
          copiedArticle       = article.copy(
            notes = user
              .map(u => EditorNote("Artikkelen har blitt lagret som ny versjon", u.id, article.status, clock.now()))
              .toList,
            previousVersionsNotes = article.previousVersionsNotes ++ article.notes,
            responsible =
              if (keepDraftData) article.responsible
              else None,
            priority =
              if (keepDraftData) article.priority
              else Priority.Unspecified,
            comments =
              if (keepDraftData | article.articleType == ArticleType.TopicArticle) article.comments
              else Seq.empty,
          )
          dataObject = {
            val obj = new PGobject()
            obj.setType("jsonb")
            obj.setValue(CirceUtil.toJsonString(copiedArticle))
            obj
          }
          uuid = Try(importId.map(UUID.fromString)).toOption.flatten
          slug = article.slug.map(_.toLowerCase)
          _   <- tsql"""
            insert into ${DBArticle.table} (external_id, external_subject_id, document, revision, import_id, article_id, slug)
            values (ARRAY[$externalIds]::text[],
                    ARRAY[$externalSubjectIds]::text[],
                    $dataObject,
                    $articleRevision,
                    $uuid,
                    $articleId,
                    $slug)
          """
            .updateAndReturnGeneratedKey()
            .map { dbId =>
              logger.info(s"Inserted new article: $articleId (with db id $dbId)")
            }
        } yield copiedArticle.copy(revision = Some(articleRevision))
    }
  }

  def newEmptyArticleId()(using session: DBSession): Try[Long] = {
    tsql"SELECT NEXTVAL('article_id_sequence') as article_id"
      .map(rs => rs.long("article_id"))
      .runSingleTry(GenerateIDException("No id gotten when generating id in postgresql statement, this is weird."))
      .map { articleId =>
        logger.info(s"Generated new article id: $articleId")
        articleId
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

  def updateArticle(article: Draft)(using session: DBSession): Try[Draft] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(article))

    val oldRevision = article.revision.getOrElse(0)
    val newRevision = oldRevision + 1
    val slug        = article.slug.map(_.toLowerCase)

    val whereClause = sqls"""
      where article_id=${article.id}
      and revision=$oldRevision
      and revision=(select max(revision) from ${DBArticle.table} where article_id=${article.id})
    """

    for {
      oldNotes <- tsql"""
        select document->'notes' as notes
        from ${DBArticle.table}
        $whereClause
        for update
      """.map(editorNotesFromRS).runSingle()
      notes = oldNotes match {
        case Some(n) => n ++ article.notes
        case None    => article.notes
      }
      count <- tsql"""
        update ${DBArticle.table}
        set document=jsonb_set($dataObject,'{notes}',(${CirceUtil.toJsonString(notes.distinct)}::jsonb)),
            revision=$newRevision,
            slug=$slug
        $whereClause
      """.update()
      updated <- failIfRevisionMismatch(count, article, newRevision)
    } yield updated
  }

  private def editorNotesFromRS(rs: WrappedResultSet): Seq[EditorNote] = {
    Option(rs.string("notes")).map(CirceUtil.unsafeParseAs[Seq[EditorNote]](_)).getOrElse(Seq.empty)
  }

  def updateArticleNotes(articleId: Long, notes: Seq[EditorNote])(using session: DBSession): Try[Boolean] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(notes))

    tsql"""
      update ${DBArticle.table}
      set document=jsonb_set(document, '{notes}',(document -> 'notes') || $dataObject)
      where article_id=$articleId
      and revision=(select max(revision) from ${DBArticle.table} where article_id=$articleId)
    """
      .update()
      .flatMap {
        case 1 => Success(true)
        case _ => Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
  }

  def withId(articleId: Long)(using session: DBSession): Try[Option[Draft]] = articleWhere(sqls"""
    ar.article_id=${articleId.toInt}
    ORDER BY revision
    DESC LIMIT 1
  """)

  def withIds(articleIds: List[Long], offset: Long, pageSize: Long)(using session: DBSession): Try[Seq[Draft]] = {
    val ar  = DBArticle.syntax("ar")
    val ar2 = DBArticle.syntax("ar2")
    tsql"""
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
    """.map(DBArticle.fromResultSet(ar)).runList()
  }

  def idsWithStatus(status: DraftStatus)(using session: DBSession): Try[List[ArticleIds]] = {
    val ar = DBArticle.syntax("ar")
    tsql"""
      select article_id, external_id
      from ${DBArticle.as(ar)}
      where ar.document is not NULL and ar.document#>>'{status,current}' = ${status.toString}
    """.map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs))).runList()
  }

  def exists(id: Long)(using session: DBSession): Try[Boolean] = {
    tsql"select article_id from ${DBArticle.table} where article_id=$id order by revision desc limit 1"
      .map(rs => rs.long("article_id"))
      .runSingle()
      .map(_.isDefined)
  }

  def deleteArticle(articleId: Long)(using session: DBSession): Try[Long] = {
    tsql"delete from ${DBArticle.table} where article_id = $articleId"
      .update()
      .flatMap {
        case 1 => Success(articleId)
        case _ => Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
  }

  def deleteArticleRevision(articleId: Long, revision: Int)(using session: DBSession): Try[Unit] =
    tsql"delete from ${DBArticle.table} where article_id = $articleId and revision = $revision"
      .update()
      .flatMap {
        case 1 => Success(())
        case _ => Failure(NotFoundException(s"Article with id $articleId and revision $revision does not exist"))
      }

  def getCurrentAndPreviousRevision(articleId: Long)(using session: DBSession): Try[(Draft, Draft)] = {
    val ar = DBArticle.syntax("ar")
    tsql"""
      select ${ar.result.*}
      from ${DBArticle.as(ar)}
      where ar.article_id = $articleId
      order by revision desc
      limit 2
    """
      .map(DBArticle.fromResultSet(ar))
      .runList()
      .flatMap {
        case List(current, previous) => Success((current, previous))
        case _                       => Failure(NotFoundException(s"Article with id $articleId has fewer than 2 revisions"))
      }
  }

  def getIdFromExternalId(externalId: String)(using session: DBSession): Try[Option[Long]] = {
    tsql"""
      select article_id
      from ${DBArticle.table}
      where $externalId = any (external_id)
      order by revision desc
      limit 1
    """.map(rs => rs.long("article_id")).runSingle()
  }

  private def externalIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
    Option(wrappedResultSet.array("external_id"))
      .map(_.getArray.asInstanceOf[Array[String]])
      .getOrElse(Array.empty[String])
      .toList
      .flatMap(Option(_))
  }

  def getExternalIdsFromId(id: Long)(using session: DBSession): Try[List[String]] = {
    tsql"""
      select external_id
      from ${DBArticle.table}
      where article_id=${id.toInt}
      order by revision desc
      limit 1
    """.map(externalIdsFromResultSet).runSingle().map(_.getOrElse(List.empty))
  }

  private def externalSubjectIdsFromResultSet(wrappedResultSet: WrappedResultSet): List[String] = {
    Option(wrappedResultSet.array("external_subject_id"))
      .map(_.getArray.asInstanceOf[Array[String]])
      .getOrElse(Array.empty[String])
      .toList
      .flatMap(Option(_))
  }

  def getExternalSubjectIdsFromId(id: Long)(using session: DBSession): Try[Seq[String]] = {
    tsql"""
      select external_subject_id
      from ${DBArticle.table}
      where article_id=${id.toInt}
      order by revision desc
      limit 1
    """.map(externalSubjectIdsFromResultSet).runSingle().map(_.getOrElse(List.empty))
  }

  def getImportIdFromId(id: Long)(using session: DBSession): Try[Option[String]] = {
    tsql"""
      select import_id
      from ${DBArticle.table}
      where article_id=${id.toInt}
      order by revision desc
      limit 1
    """.map(rs => rs.string("import_id")).runSingle()
  }

  def getAllIds(using session: DBSession): Try[Seq[ArticleIds]] = {
    tsql"select article_id, max(external_id) as external_id from ${DBArticle.table} group by article_id order by article_id asc"
      .map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs)))
      .runList()
  }

  def articleCount(using session: DBSession): Try[Long] = {
    tsql"select count(distinct article_id) from ${DBArticle.table} where document is not NULL"
      .map(rs => rs.long("count"))
      .runSingle()
      .map(_.getOrElse(0))
  }

  def getArticlesByPage(pageSize: Int, offset: Int)(using session: DBSession): Try[Seq[Draft]] = {
    val ar = DBArticle.syntax("ar")
    tsql"""
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
    """.map(DBArticle.fromResultSet(ar)).runList()
  }

  def minMaxArticleId(using session: DBSession): Try[(Long, Long)] = {
    tsql"select coalesce(MIN(article_id),0) as mi, coalesce(MAX(article_id),0) as ma from ${DBArticle.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
  }

  override def minMaxId(using session: DBSession): Try[(Long, Long)] = {
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${DBArticle.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
  }

  def documentsWithArticleIdBetween(min: Long, max: Long)(using session: DBSession): Try[List[Draft]] = {
    val ar       = DBArticle.syntax("ar")
    val subquery = DBArticle.syntax("b")
    tsql"""
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
    """.map(DBArticle.fromResultSet(ar)).runList()
  }

  override def documentsWithIdBetween(min: Long, max: Long)(using session: DBSession): Try[List[Draft]] = {
    val ar       = DBArticle.syntax("ar")
    val subquery = DBArticle.syntax("b")
    tsql"""
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
    """.map(DBArticle.fromResultSet(ar)).runList()
  }

  private def articleWhere(whereClause: SQLSyntax)(using session: DBSession): Try[Option[Draft]] = {
    val ar = DBArticle.syntax("ar")

    tsql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.document is not NULL and $whereClause "
      .map(DBArticle.fromResultSet(ar))
      .runSingle()
  }

  def articlesWithId(articleId: Long)(using session: DBSession): Try[List[Draft]] =
    articlesWhere(sqls"ar.article_id = $articleId").map(_.toList)

  private def articlesWhere(whereClause: SQLSyntax)(using session: DBSession): Try[Seq[Draft]] = {
    val ar = DBArticle.syntax("ar")
    tsql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.document is not NULL and $whereClause"
      .map(DBArticle.fromResultSet(ar))
      .runList()
  }

  def importIdOfArticle(externalId: String)(using session: DBSession): Try[Option[ImportId]] = {
    val ar = DBArticle.syntax("ar")
    tsql"""select ${ar.result.*}, import_id, external_id
           from ${DBArticle.as(ar)}
           where ar.document is not NULL and $externalId = any (ar.external_id)"""
      .map(rs => ImportId(rs.stringOpt("import_id")))
      .runSingle()
  }

  def withSlug(slug: String)(using session: DBSession): Try[Option[Draft]] =
    articleWhere(sqls"ar.slug=${slug.toLowerCase} ORDER BY revision DESC LIMIT 1")

  def slugExists(slug: String, articleId: Option[Long])(using session: DBSession): Try[Boolean] = {
    val sq = articleId match {
      case None     => tsql"select count(*) from ${DBArticle.table} where slug = ${slug.toLowerCase}"
      case Some(id) =>
        tsql"select count(*) from ${DBArticle.table} where slug = ${slug.toLowerCase} and article_id != $id"
    }
    sq.map(rs => rs.long("count")).runSingle().map(_.exists(_ > 0))
  }

  def getAllResponsibles(using session: DBSession): Try[Seq[String]] = {
    val ar = DBArticle.syntax("ar")
    tsql"""
      select distinct (ar.document -> 'responsible' ->> 'responsibleId') as responsibleId
      from ${DBArticle.as(ar)}
      where ar.document is not NULL
      and (ar.document -> 'responsible') is not null
      and (ar.document -> 'responsible' ->> 'responsibleId') is not null
    """.map(rs => rs.string("responsibleId")).runList()
  }

}
