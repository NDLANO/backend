/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import io.circe.parser
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.Tag
import no.ndla.database.DocumentMigration
import no.ndla.network.clients.TaxonomyApiClient

class V62__SetResourceTypeFromTaxonomyAsTag()(using taxonomyClient: TaxonomyApiClient) extends DocumentMigration {
  override val tableName: String  = "contentdata"
  override val columnName: String = "document"

  private val taxonomyBundle = taxonomyClient.getTaxonomyBundleUncached(true).get

  override def convertColumn(value: String): String = {

    val oldDocument = parser.parse(value).toTry.get
    val articleId   = oldDocument.hcursor.downField("id").as[Long].toTry.get
    val node        = taxonomyBundle.nodeByContentUri.get(s"urn:article:$articleId") match {
      case Some(n) => n.headOption
      case None    => return value
    }
    val resourceTypes =
      node
        .flatMap(n => n.context.map(c => c.resourceTypes.filter(rt => rt.parentId.isDefined).map(rt => rt.name)))
        .getOrElse(List.empty)
    val tags = oldDocument.hcursor.downField("tags").as[Option[Seq[Tag]]].toTry.get.getOrElse(Seq.empty)
    // Insert values from searchablelanguagevalues as tags if they are not already present
    // A Tag has a language and a sequence of strings. A SearchableLanguageValues has a language and a single string
    // We add the value from searchablelanguagevalues to the tags if it is not already present in the tags for the language from tag
    val newTags = resourceTypes.iterator.foldLeft(tags) { (acc, rt) =>
      val languages = acc.map(_.language).distinct
      languages.foldLeft(acc) { (innerAcc, lang) =>
        val existingTag = innerAcc.find(t => t.language == lang)
        existingTag match {
          case Some(_) =>
            innerAcc.find(t => t.language == lang) match {
              case Some(tag) =>
                val updatedTags =
                  tag.tags :+ rt.languageValues.find(lv => lv.language == lang).map(_.value).getOrElse("")
                innerAcc.map(t => if (t.language == lang) t.copy(tags = updatedTags.distinct) else t)
              case None => innerAcc
            }
          case _ => innerAcc
        }
      }
    }
    val updatedDocument = oldDocument.hcursor.downField("tags").withFocus(_ => newTags.asJson)
    updatedDocument.top.get.noSpaces
  }
}
