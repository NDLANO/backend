/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.syntax.EncoderOps
import io.circe.parser
import no.ndla.database.DocumentMigration
import no.ndla.common.model.domain.learningpath.EmbedUrl

class V48__AddArticleId extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "learningsteps"
  val articleIdRegex              = """^\/article-iframe\/?.*?\/(\d+)""".r

  def convertColumn(value: String): String = {
    val oldDocument                 = parser.parse(value).toTry.get
    val embedUrl                    = oldDocument.hcursor.downField("embedUrl").as[Seq[EmbedUrl]].toTry.get
    val oldArticle                  = oldDocument.hcursor.downField("articleId").as[Option[Long]].toTry.getOrElse(None)
    val (newArticles, newEmbedUrls) =
      embedUrl.foldLeft((Set.empty[Long], Seq.empty[EmbedUrl]))((acc, url) =>
        articleIdRegex.findFirstMatchIn(url.url) match {
          case Some(matched) if matched.group(1) != null && matched.group(1).toLongOption.isDefined =>
            (acc._1 + matched.group(1).toLong, acc._2)
          case _ => (acc._1, acc._2 :+ url)
        }
      )

    if (newArticles.size > 1) {
      throw new IllegalArgumentException(
        s"Multiple articles found in embedUrl for learning step with url ${embedUrl}"
      )
    }

    val newArticleId = newArticles.headOption.orElse(oldArticle)

    val newDocument = oldDocument.hcursor
      .withFocus(_.mapObject(_.remove("embedUrl").add("embedUrl", newEmbedUrls.asJson)))
      .withFocus(_.mapObject(_.remove("articleId").add("articleId", newArticleId.asJson)))

    newDocument.top.get.noSpaces
  }
}
