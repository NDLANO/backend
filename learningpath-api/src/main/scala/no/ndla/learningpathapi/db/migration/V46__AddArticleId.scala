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
import no.ndla.common.model.domain.learningpath.LearningStepArticle
import no.ndla.common.model.domain.learningpath.EmbedUrl

class V46__AddArticleId extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "learningsteps"
  val articleIdRegex              = """^\/article-iframe\/?.*?\/(\d+)""".r

  def convertColumn(value: String): String = {
    val oldDocument = parser.parse(value).toTry.get
    val embedUrl    = oldDocument.hcursor.downField("embedUrl").as[Seq[EmbedUrl]].toTry.get
    val oldArticles = oldDocument.hcursor.downField("article").as[Seq[LearningStepArticle]].toTry.getOrElse(Seq.empty)
    val (newArticles, newEmbedUrls) =
      embedUrl.foldLeft((Seq.empty[LearningStepArticle], Seq.empty[EmbedUrl]))((acc, url) =>
        articleIdRegex.findFirstMatchIn(url.url) match {
          case Some(matched) if matched.group(1) != null && matched.group(1).toLongOption.isDefined =>
            (acc._1 :+ LearningStepArticle(language = url.language, id = matched.group(1).toLong), acc._2)
          case _ => (acc._1, acc._2 :+ url)
        }
      )

    val newDocument = oldDocument.hcursor
      .withFocus(_.mapObject(_.remove("embedUrl").add("embedUrl", newEmbedUrls.asJson)))
      .withFocus(_.mapObject(_.remove("article").add("article", oldArticles.concat(newArticles).asJson)))

    newDocument.top.get.noSpaces
  }
}
