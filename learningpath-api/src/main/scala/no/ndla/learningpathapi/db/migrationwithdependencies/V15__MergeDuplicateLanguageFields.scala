/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migrationwithdependencies

import no.ndla.learningpathapi.{LearningpathApiProperties, Props}
import no.ndla.learningpathapi.model.domain.LearningStep
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Formats
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V15__MergeDuplicateLanguageFields(properties: LearningpathApiProperties) extends BaseJavaMigration with Props {
  override val props            = properties
  implicit val formats: Formats = LearningStep.jsonEncoder

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      allLearningPaths
        .map { case (id, document) => (id, convertLearningPathDocument(id, document)) }
        .foreach { case (id, document) => updateLearningPath(id, document) }

      allLearningSteps
        .map { case (id, document) => (id, convertLearningStepDocument(id, document)) }
        .foreach { case (id, document) => updateLearningStep(id, document) }
    }

  def allLearningPaths(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningpaths"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def allLearningSteps(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningsteps"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def mergeDuplicateLanguageFields(docType: String, id: Long, doc: JValue): JValue = {
    doc.mapField {
      case (fieldName, arr: JArray) =>
        fieldName -> mergeLanguageFields(fieldName, docType, id, arr)
      case x => x
    }
  }

  def onlyLanguageFields(arr: List[JValue]): Boolean = {
    arr.forall(x => (x \ "language").toSome.isDefined)
  }

  def mergeLanguageFields(fieldName: String, docType: String, id: Long, array: JArray): JValue = {
    if (!onlyLanguageFields(array.arr)) {
      array
    } else {
      val grouped = array.arr.groupBy(v => (v \ "language").extract[String])
      val newLanguages = grouped.map { case (language, value) =>
        val dist = value.distinct
        if (dist.length > 1) {
          println(
            s"WARNING!: $docType with id '$id' had duplicate of '$fieldName' of language '$language' with different content, merging!"
          )

          // Reverse so the first one wins
          value.reverse.foldLeft(JObject()) { case (acc, cur) =>
            val obj = cur.extract[JObject]
            acc.merge(obj)
          }
        } else { value.head }
      }.toList

      JArray(newLanguages)
    }
  }

  def convertLearningStepDocument(id: Long, document: String): String = {
    val oldLearningStep = parse(document)
    val updated         = mergeDuplicateLanguageFields("learningStep", id, oldLearningStep)
    compact(render(updated))
  }

  def convertLearningPathDocument(id: Long, document: String): String = {
    val oldLearningpath = parse(document)
    val updated         = mergeDuplicateLanguageFields("learningPath", id, oldLearningpath)
    compact(render(updated))
  }

  def updateLearningPath(id: Long, document: String)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningpaths set document = $dataObject where id = $id"
      .update()
  }

  def updateLearningStep(id: Long, document: String)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningsteps set document = $dataObject where id = $id"
      .update()
  }
}
