/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import io.circe.{Json, parser}

abstract class LanguageFieldMigration extends DocumentMigration {
  protected def fieldName: String
  protected def oldSubfieldName: String = fieldName

  private def convertOldLanguageField(fields: Vector[Json]): Json = {
    fields.foldLeft(Json.obj()) { (acc, disclaimer) =>
      val language = disclaimer.hcursor.downField("language").as[String].toTry.get
      val text     = disclaimer.hcursor.downField(oldSubfieldName).as[String].toTry.get
      acc.mapObject(_.add(language, Json.fromString(text)))
    }
  }

  private def addEmptyLanguageField(obj: Json): String = {
    obj.withObject(_.add(fieldName, Json.obj()).toJson).noSpaces
  }

  override def convertColumn(document: String): String = {
    val oldArticle = parser.parse(document).toTry.get
    oldArticle.hcursor.downField(fieldName).focus match {
      case None                => addEmptyLanguageField(oldArticle)
      case Some(f) if f.isNull => addEmptyLanguageField(oldArticle)
      case Some(disclaimers) =>
        val disclaimerVector = disclaimers.asArray.get
        val converted        = convertOldLanguageField(disclaimerVector)
        val newArticle       = oldArticle.withObject(_.remove(fieldName).add(fieldName, converted).toJson)
        newArticle.noSpaces
    }
  }
}
