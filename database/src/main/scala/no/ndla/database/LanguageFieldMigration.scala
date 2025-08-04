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
    fields.foldLeft(Json.obj()) { (acc, field) =>
      val language = field.hcursor.downField("language").as[String].toTry.get
      val text     = field.hcursor.downField(oldSubfieldName).as[String].toTry.get
      acc.mapObject(_.add(language, Json.fromString(text)))
    }
  }

  private def addEmptyLanguageField(obj: Json): String = {
    obj.withObject(_.add(fieldName, Json.obj()).toJson).noSpaces
  }

  override def convertColumn(document: String): String = {
    val oldDocument = parser.parse(document).toTry.get
    oldDocument.hcursor.downField(fieldName).focus match {
      case None                => addEmptyLanguageField(oldDocument)
      case Some(f) if f.isNull => addEmptyLanguageField(oldDocument)
      case Some(values)        =>
        val valueVector = values.asArray.get
        val converted   = convertOldLanguageField(valueVector)
        val newArticle  = oldDocument.withObject(_.remove(fieldName).add(fieldName, converted).toJson)
        newArticle.noSpaces
    }
  }
}
