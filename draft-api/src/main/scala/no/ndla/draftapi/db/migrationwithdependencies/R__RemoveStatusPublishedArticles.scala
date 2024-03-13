/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import enumeratum.Json4s
import no.ndla.draftapi.{DraftApiProperties, Props}
import no.ndla.draftapi.model.domain.DBArticle
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.domain.{ArticleType, Status}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

class R__RemoveStatusPublishedArticles(properties: DraftApiProperties)
    extends BaseJavaMigration
    with DBArticle
    with Props {
  override val props: DraftApiProperties = properties

  implicit val formats: Formats =
    org.json4s.DefaultFormats + Json4s.serializer(DraftStatus) + Json4s.serializer(ArticleType)

  override def getChecksum: Integer = 0 // Change this to something else if you want to repeat migration

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L
    while (numPagesLeft > 0) {
      allArticles(offset * 1000).foreach(updateArticle)
      numPagesLeft -= 1
      offset += 1
    }

  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[Draft] = {
    val ar = DBArticle.syntax("ar")
    sql"select ${ar.result.*} from ${DBArticle.as(ar)} where ar.document is not NULL order by ar.id limit 1000 offset $offset"
      .map(DBArticle.fromResultSet(ar))
      .list()
  }

  def updateArticle(article: Draft)(implicit session: DBSession): Int = {
    val newArticle = article.copy(status = updateStatus(article.status))
    saveArticle(newArticle)
  }

  def updateStatus(status: Status): Status = {
    if (status.current == DraftStatus.PUBLISHED) {
      val newOther: Set[DraftStatus] = status.other.filter(value => value == DraftStatus.IMPORTED)
      status.copy(other = newOther)
    } else status
  }

  def saveArticle(article: Draft)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(article))

    sql"update articledata set document = $dataObject where article_id=${article.id}"
      .update()
  }

}
