package no.ndla.articleapi.db.migration

import io.circe.{Json, parser}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import scalikejdbc.{DB, DBSession, *}
import org.postgresql.util.PGobject

class V55__AddArticleIntroSummary extends BaseJavaMigration {

  def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allRows(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from contentdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateRow(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)
    sql"update contentdata set document = $dataObject where id = $id"
      .update()
  }

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session => migrateRows(session) }

  def migrateRows(implicit session: DBSession): Unit = {
    val count        = countAllRows.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L
    while (numPagesLeft > 0) {
      allRows(offset * 1000).map { case (id, document) =>
        updateRow(convertDocument(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  private[migration] def convertDocument(document: String): String = {
    val oldArticle = parser.parse(document).toTry.get
    val cursor     = oldArticle.hcursor
    val updatedCursor = cursor.withFocus(_.mapObject(obj => {
      obj.add(
        "summary",
        Json.arr(
          Json.obj(
            "summary"  -> Json.fromString(""),
            "language" -> Json.fromString("nb")
          )
        )
      )
    }))
    updatedCursor.top.map(_.noSpaces).getOrElse(document)
  }

}