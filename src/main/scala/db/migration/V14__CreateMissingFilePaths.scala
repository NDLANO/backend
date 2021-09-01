package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JArray, JValue}
import org.json4s.{JNothing, JObject, JString, JValue}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.native.Serialization
import org.postgresql.util.PGobject
import scalikejdbc._

class V14__CreateMissingFilePaths extends BaseJavaMigration {
  implicit val formats = org.json4s.DefaultFormats
  case class languageObject(language: String)
  case class filePathObject(filePath: String, fileSize: Long, language: String, mimeType: String)

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map {
        case (id: Long, document: String) => update(convertDocument(document), id)
      }
    }
  }

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from audiodata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
      .apply()
  }

  def convertDocument(document: String): String = {

    val oldArticle = parse(document)
    val newArticle = oldArticle.mapField {
      case ("filePaths", filePaths: JArray) =>
        "filePaths" -> {
          if (filePaths.children.length.equals(0)) {
            JArray(List.empty[JValue])
          } else {
            JArray({
              val supportedLanguages = ((oldArticle \ "tags") ++ (oldArticle \ "titles") ++ (oldArticle \ "filePaths"))
                .extract[List[languageObject]]
                .map(f => f.language)
                .distinct
              supportedLanguages.map(supportedLang => {
                val filePath = filePaths.children.find((fp) => {
                  val lang = fp
                    .findField((field) => {
                      field._1.equals("language")
                    })
                    .get
                    ._2
                    .asInstanceOf[JString]

                  lang.s.equals(supportedLang)

                })
                filePath match {
                  case Some(file) => {
                    file
                  }
                  case None =>
                    val fileObjects = filePaths.extract[List[filePathObject]]
                    val newFilePath = fileObjects.find(fp => fp.language.equals("nb")).getOrElse(fileObjects.head)
                    parse(Serialization.write(newFilePath.copy(language = supportedLang)))

                }

              })
            })
          }
        }

      case x => x

    }

    compact(render(newArticle))
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = ${dataObject} where id = $id".update().apply()
  }

}
