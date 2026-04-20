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

class DraftRepository(using draftErrorHelpers: DraftErrorHelpers, clock: Clock, dbDraft: DBDraft)
    extends StrictLogging
    with Repository[Draft] {
  import draftErrorHelpers.*

  def insert(article: Draft)(using session: DBSession): Try[Draft] = {
    val startRevision = article.revision.getOrElse(1)
    val dataObject    = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(article))
    val slug                                  = article.slug.map(_.toLowerCase)
    val (responsibleId, responsibleUpdatedAt) = article
      .responsible
      .map(r => (r.responsibleId, r.lastUpdated.toTimestamptzParameterBinder))
      .unzip

    for {
      dbId <- tsql"""
        insert into ${dbDraft.table} (document, revision, external_id, article_id, slug, responsible, responsible_updated_at)
        values ($dataObject, $startRevision, ARRAY[${article.externalIds.getOrElse(List.empty)}]::text[], ${article.id}, $slug, $responsibleId, $responsibleUpdatedAt)
      """.updateAndReturnGeneratedKey()
      inserted = {
        logger.info(s"Inserted new article: ${article.id}, with revision $startRevision (with db id $dbId)")
        article.copy(revision = Some(startRevision), slug = slug)
      }
      tracked <- trackEditor(inserted)
    } yield tracked
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
            comments =
              if (keepDraftData | article.articleType == ArticleType.TopicArticle) article.comments
              else Seq.empty,
            priority =
              if (keepDraftData) article.priority
              else Priority.Unspecified,
          )
          dataObject = {
            val obj = new PGobject()
            obj.setType("jsonb")
            obj.setValue(CirceUtil.toJsonString(copiedArticle))
            obj
          }
          uuid                                  = Try(importId.map(UUID.fromString)).toOption.flatten
          slug                                  = article.slug.map(_.toLowerCase)
          (responsibleId, responsibleUpdatedAt) = copiedArticle
            .responsible
            .map(r => (r.responsibleId, r.lastUpdated.toTimestamptzParameterBinder))
            .unzip
          _ <- tsql"""
            insert into ${dbDraft.table} (external_id, external_subject_id, document, revision, import_id, article_id, slug, responsible, responsible_updated_at)
            values (ARRAY[${article.externalIds.getOrElse(List.empty)}]::text[],
                    ARRAY[$externalSubjectIds]::text[],
                    $dataObject,
                    $articleRevision,
                    $uuid,
                    $articleId,
                    $slug,
                    $responsibleId,
                    $responsibleUpdatedAt)
          """
            .updateAndReturnGeneratedKey()
            .map { dbId =>
              logger.info(s"Inserted new article: $articleId (with db id $dbId)")
            }
          copiedArticleWithRevision = copiedArticle.copy(revision = Some(articleRevision))
          trackedArticle           <- trackEditor(copiedArticleWithRevision)
        } yield trackedArticle
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

    val oldRevision                           = article.revision.getOrElse(0)
    val newRevision                           = oldRevision + 1
    val slug                                  = article.slug.map(_.toLowerCase)
    val (responsibleId, responsibleUpdatedAt) = article
      .responsible
      .map(r => (r.responsibleId, r.lastUpdated.toTimestamptzParameterBinder))
      .unzip

    val whereClause = sqls"""
      where dr.article_id=${article.id}
      and dr.revision=$oldRevision
      and not exists (select 1 from ${dbDraft.table} dr2 where dr2.article_id = dr.article_id and dr2.revision > dr.revision)
    """

    for {
      oldNotes <- tsql"""
        select dr.document->'notes' as notes
        from ${dbDraft.table} dr
        $whereClause
        for update
      """.map(editorNotesFromRS).runSingle()
      notes = oldNotes match {
        case Some(n) => n ++ article.notes
        case None    => article.notes
      }
      count <- tsql"""
        update ${dbDraft.table} dr
        set document=jsonb_set($dataObject,'{notes}',(${CirceUtil.toJsonString(notes.distinct)}::jsonb)),
            revision=$newRevision,
            slug=$slug,
            responsible=$responsibleId,
            responsible_updated_at=$responsibleUpdatedAt
        $whereClause
      """.update()
      updated <- failIfRevisionMismatch(count, article, newRevision)
      tracked <- trackEditor(updated)
    } yield tracked
  }

  private def editorNotesFromRS(rs: WrappedResultSet): Seq[EditorNote] = {
    Option(rs.string("notes")).map(CirceUtil.unsafeParseAs[Seq[EditorNote]](_)).getOrElse(Seq.empty)
  }

  def updateArticleNotes(articleId: Long, notes: Seq[EditorNote])(using session: DBSession): Try[Boolean] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(notes))

    tsql"""
      update ${dbDraft.table} dr
      set document=jsonb_set(dr.document, '{notes}',(dr.document -> 'notes') || $dataObject)
      where dr.article_id=$articleId
      and not exists (select 1 from ${dbDraft.table} dr2 where dr2.article_id = dr.article_id and dr2.revision > dr.revision)
    """
      .update()
      .flatMap {
        case 1 => Success(true)
        case _ => Failure(NotFoundException(s"Article with id $articleId does not exist"))
      }
  }

  def withId(articleId: Long)(using session: DBSession): Try[Option[Draft]] = articleWhere(sqls"""
    dr.article_id=${articleId.toInt}
    ORDER BY revision
    DESC LIMIT 1
  """)

  def withIds(articleIds: List[Long], offset: Long, pageSize: Long)(using session: DBSession): Try[Seq[Draft]] = {
    val dr  = dbDraft.syntax("dr")
    val dr2 = dbDraft.syntax("dr2")
    tsql"""
      select ${dr.result.*}
      from ${dbDraft.as(dr)}
      where dr.document is not NULL
      and dr.article_id in ($articleIds)
      and not exists (
          select 1
          from ${dbDraft.as(dr2)}
          where dr2.article_id = dr.article_id
          and dr2.revision > dr.revision
      )
      order by dr.article_id
      offset $offset
      limit $pageSize
    """.map(dbDraft.fromResultSet(dr)).runList()
  }

  def idsWithStatus(status: DraftStatus)(using session: DBSession): Try[List[ArticleIds]] = {
    val dr = dbDraft.syntax("dr")
    tsql"""
      select article_id, external_id
      from ${dbDraft.as(dr)}
      where dr.document is not NULL and dr.document#>>'{status,current}' = ${status.toString}
    """.map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs))).runList()
  }

  def exists(id: Long)(using session: DBSession): Try[Boolean] = {
    tsql"select article_id from ${dbDraft.table} where article_id=$id order by revision desc limit 1"
      .map(rs => rs.long("article_id"))
      .runSingle()
      .map(_.isDefined)
  }

  def deleteArticle(articleId: Long)(using session: DBSession): Try[Long] = {
    for {
      result <- tsql"delete from ${dbDraft.table} where article_id = $articleId"
        .update()
        .flatMap {
          case 1 => Success(articleId)
          case _ => Failure(NotFoundException(s"Article with id $articleId does not exist"))
        }
      _ <- tsql"delete from draft_editors where draft_id = $articleId".update()
    } yield result
  }

  def deleteArticleRevision(articleId: Long, revision: Int)(using session: DBSession): Try[Unit] =
    tsql"delete from ${dbDraft.table} where article_id = $articleId and revision = $revision"
      .update()
      .flatMap {
        case 1 => Success(())
        case _ => Failure(NotFoundException(s"Article with id $articleId and revision $revision does not exist"))
      }

  def getCurrentAndPreviousRevision(articleId: Long)(using session: DBSession): Try[(Draft, Draft)] = {
    val dr = dbDraft.syntax("dr")
    tsql"""
      select ${dr.result.*}
      from ${dbDraft.as(dr)}
      where dr.article_id = $articleId
      order by revision desc
      limit 2
    """
      .map(dbDraft.fromResultSet(dr))
      .runList()
      .flatMap {
        case List(current, previous) => Success((current, previous))
        case _                       => Failure(NotFoundException(s"Article with id $articleId has fewer than 2 revisions"))
      }
  }

  def getIdFromExternalId(externalId: String)(using session: DBSession): Try[Option[Long]] = {
    tsql"""
      select article_id
      from ${dbDraft.table}
      where $externalId = any (external_id)
      order by revision desc
      limit 1
    """.map(rs => rs.long("article_id")).runSingle()
  }

  private def externalIdsFromResultSet(wrappedResultSet: WrappedResultSet): Option[List[String]] = {
    wrappedResultSet
      .arrayOpt("external_id")
      .map(_.getArray.asInstanceOf[Array[String]].toList.filter(_ != null))
      .flatMap {
        case Nil  => None
        case list => Some(list)
      }
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
      from ${dbDraft.table}
      where article_id=${id.toInt}
      order by revision desc
      limit 1
    """.map(externalSubjectIdsFromResultSet).runSingle().map(_.getOrElse(List.empty))
  }

  def getImportIdFromId(id: Long)(using session: DBSession): Try[Option[String]] = {
    tsql"""
      select import_id
      from ${dbDraft.table}
      where article_id=${id.toInt}
      order by revision desc
      limit 1
    """.map(rs => rs.string("import_id")).runSingle()
  }

  def getAllIds(using session: DBSession): Try[Seq[ArticleIds]] = {
    tsql"select article_id, max(external_id) as external_id from ${dbDraft.table} group by article_id order by article_id asc"
      .map(rs => ArticleIds(rs.long("article_id"), externalIdsFromResultSet(rs)))
      .runList()
  }

  def articleCount(using session: DBSession): Try[Long] = {
    tsql"select count(distinct article_id) from ${dbDraft.table} where document is not NULL"
      .map(rs => rs.long("count"))
      .runSingle()
      .map(_.getOrElse(0))
  }

  def getArticlesByPage(pageSize: Int, offset: Int)(using session: DBSession): Try[Seq[Draft]] = {
    val dr  = dbDraft.syntax("dr")
    val dr2 = dbDraft.syntax("dr2")
    tsql"""
      select ${dr.result.*}
      from ${dbDraft.as(dr)}
      where dr.document is not NULL
      and not exists (
        select 1
        from ${dbDraft.as(dr2)}
        where dr2.article_id = dr.article_id
        and dr2.revision > dr.revision
      )
      order by dr.id
      offset $offset
      limit $pageSize
    """.map(dbDraft.fromResultSet(dr)).runList()
  }

  def minMaxArticleId(using session: DBSession): Try[(Long, Long)] = {
    tsql"select coalesce(MIN(article_id),0) as mi, coalesce(MAX(article_id),0) as ma from ${dbDraft.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
  }

  override def minMaxId(using session: DBSession): Try[(Long, Long)] = {
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${dbDraft.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
  }

  def documentsWithArticleIdBetween(min: Long, max: Long)(using session: DBSession): Try[List[Draft]] = {
    val dr  = dbDraft.syntax("dr")
    val dr2 = dbDraft.syntax("dr2")
    tsql"""
      select ${dr.result.*}
      from ${dbDraft.as(dr)}
      where dr.document is not NULL
      and dr.article_id between $min and $max
      and dr.document#>>'{status,current}' <> ${DraftStatus.ARCHIVED.toString}
      and not exists (
        select 1
        from ${dbDraft.as(dr2)}
        where dr2.article_id = dr.article_id
        and dr2.revision > dr.revision
      )
    """.map(dbDraft.fromResultSet(dr)).runList()
  }

  override def documentsWithIdBetween(min: Long, max: Long)(using session: DBSession): Try[List[Draft]] = {
    val dr  = dbDraft.syntax("dr")
    val dr2 = dbDraft.syntax("dr2")
    tsql"""
      select ${dr.result.*}
      from ${dbDraft.as(dr)}
      where dr.document is not NULL
      and dr.id between $min and $max
      and dr.document#>>'{status,current}' <> ${DraftStatus.ARCHIVED.toString}
      and not exists (
        select 1
        from ${dbDraft.as(dr2)}
        where dr2.article_id = dr.article_id
        and dr2.revision > dr.revision
      )
    """.map(dbDraft.fromResultSet(dr)).runList()
  }

  private def articleWhere(whereClause: SQLSyntax)(using session: DBSession): Try[Option[Draft]] = {
    val dr = dbDraft.syntax("dr")

    tsql"select ${dr.result.*} from ${dbDraft.as(dr)} where dr.document is not NULL and $whereClause "
      .map(dbDraft.fromResultSet(dr))
      .runSingle()
  }

  def articlesWithId(articleId: Long)(using session: DBSession): Try[List[Draft]] =
    articlesWhere(sqls"dr.article_id = $articleId").map(_.toList)

  private def articlesWhere(whereClause: SQLSyntax)(using session: DBSession): Try[Seq[Draft]] = {
    val dr = dbDraft.syntax("dr")
    tsql"select ${dr.result.*} from ${dbDraft.as(dr)} where dr.document is not NULL and $whereClause"
      .map(dbDraft.fromResultSet(dr))
      .runList()
  }

  def importIdOfArticle(externalId: String)(using session: DBSession): Try[Option[ImportId]] = {
    val dr = dbDraft.syntax("dr")
    tsql"""select ${dr.result.*}, import_id, external_id
           from ${dbDraft.as(dr)}
           where dr.document is not NULL and $externalId = any (dr.external_id)"""
      .map(rs => ImportId(rs.stringOpt("import_id")))
      .runSingle()
  }

  def withSlug(slug: String)(using session: DBSession): Try[Option[Draft]] =
    articleWhere(sqls"dr.slug=${slug.toLowerCase} ORDER BY revision DESC LIMIT 1")

  def slugExists(slug: String, articleId: Option[Long])(using session: DBSession): Try[Boolean] = {
    val sq = articleId match {
      case None     => tsql"select count(*) from ${dbDraft.table} where slug = ${slug.toLowerCase}"
      case Some(id) =>
        tsql"select count(*) from ${dbDraft.table} where slug = ${slug.toLowerCase} and article_id != $id"
    }
    sq.map(rs => rs.long("count")).runSingle().map(_.exists(_ > 0))
  }

  def getAllResponsibles(using session: DBSession): Try[Seq[String]] = {
    tsql"""select distinct responsible from ${dbDraft.table}""".foldLeft(Seq.empty[String]) { (acc, rs) =>
      acc ++ rs.stringOpt("responsible")
    }
  }

  def getAllEditors(using session: DBSession): Try[Seq[String]] = {
    tsql"select distinct user_id from draft_editors".map(rs => rs.string("user_id")).runList()
  }

  private def trackEditor(draft: Draft)(using session: DBSession): Try[Draft] = draft
    .id
    .map { id =>
      tsql"""
        insert into draft_editors (draft_id, user_id)
        values ($id, ${draft.updatedBy})
        on conflict do nothing
      """.update()
    }
    .getOrElse(Success(()))
    .map(_ => draft)
}
