/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.{Extraction, Formats}
import org.json4s.JsonAST.JArray
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc._

class V14__CreateMissingFilePaths extends BaseJavaMigration {
  implicit val formats: Formats = org.json4s.DefaultFormats
  case class LanguageObject(language: String)
  case class FilePathObject(filePath: String, fileSize: Long, language: String, mimeType: String)

  override def migrate(context: Context): Unit = {
    DB(context.getConnection)
      .autoClose(false)
      .withinTx { implicit session =>
        allAudios.map { case (id: Long, document: String) =>
          update(convertDocument(document), id)
        }
      }: Unit
  }

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from audiodata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
  }

  def findLanguagePrioritized(sequence: Seq[FilePathObject], language: String): Option[FilePathObject] = {
    sequence
      .find(_.language == language)
      .orElse(sequence.sortBy(lf => languagePriority.reverse.indexOf(lf.language)).lastOption)
  }

  val languagePriority: List[String] = List(
    "nb",
    "nn",
    "unknown",
    "en",
    "fr",
    "de",
    "se",
    "sma",
    "es",
    "zh"
  )

  def convertDocument(document: String): String = {
    val oldArticle = parse(document)
    val newArticle = oldArticle.mapField {
      case ("filePaths", filePaths: JArray) =>
        val oldFilePaths = filePaths.extract[List[FilePathObject]]
        val supportedLanguages = ((oldArticle \ "tags") ++ (oldArticle \ "titles") ++ (oldArticle \ "filePaths"))
          .extract[List[LanguageObject]]
          .map(_.language)
          .distinct
        val newFilePaths = supportedLanguages.flatMap(lang => {
          findLanguagePrioritized(oldFilePaths, lang)
            .map(_.copy(language = lang))
        })
        "filePaths" -> Extraction.decompose(newFilePaths)
      case x => x
    }
    compact(render(newArticle))
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = ${dataObject} where id = $id".update()
  }

}
