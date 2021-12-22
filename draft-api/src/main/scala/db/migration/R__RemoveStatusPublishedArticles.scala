/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration
import no.ndla.draftapi.model.domain.{Article, ArticleStatus, ArticleType, Status}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class R__RemoveStatusPublishedArticles extends BaseJavaMigration {

  implicit val formats = org.json4s.DefaultFormats + new EnumNameSerializer(ArticleStatus) + new EnumNameSerializer(
    ArticleType)

  override def getChecksum: Integer = 0 // Change this to something else if you want to repeat migration

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L
    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map(updateArticle)
      numPagesLeft -= 1
      offset += 1
    }

  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[Article] = {
    val ar = Article.syntax("ar")
    sql"select ${ar.result.*} from ${Article.as(ar)} where ar.document is not NULL order by ar.id limit 1000 offset $offset"
      .map(Article.fromResultSet(ar))
      .list()
  }

  def updateArticle(article: Article)(implicit session: DBSession) = {
    val newArticle = article.copy(status = updateStatus(article.status))
    saveArticle(newArticle)
  }

  def updateStatus(status: Status): Status = {
    if (status.current == ArticleStatus.PUBLISHED) {
      val newOther: Set[ArticleStatus.Value] = status.other.filter(value => value == ArticleStatus.IMPORTED)
      status.copy(other = newOther)
    } else status
  }

  def saveArticle(article: Article)(implicit session: DBSession): Long = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(article))

    sql"update articledata set document = $dataObject where article_id=${article.id}"
      .update()
  }

}
