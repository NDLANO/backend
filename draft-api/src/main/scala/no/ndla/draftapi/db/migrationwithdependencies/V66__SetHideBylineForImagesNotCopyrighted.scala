/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.draftapi.Props
import no.ndla.draftapi.db.HtmlMigration
import no.ndla.draftapi.integration.ImageApiClient
import no.ndla.network.NdlaClient
import org.jsoup.nodes.Element
import scalikejdbc.{SQLSyntax, scalikejdbcSQLInterpolationImplicitDef}

trait V66__SetHideBylineForImagesNotCopyrighted {
  this: ImageApiClient & NdlaClient & Props =>

  class V66__SetHideBylineForImagesNotCopyrighted extends HtmlMigration {
    override val tableName: String            = "articledata a"
    override val columnName: String           = "document"
    private lazy val columnNameSQL: SQLSyntax = SQLSyntax.createUnsafely(columnName)

    override lazy val whereClause: SQLSyntax =
      sqls"$columnNameSQL is not null and $columnNameSQL -> 'status' ->> 'current' != 'ARCHIVED' and $columnNameSQL -> 'status' ->> 'current' != 'UNPUBLISHED' and revision = (select max(revision) from articledata a2 where a2.article_id = a.article_id)"

    /** Method to override that manipulates the content string */
    override def convertHtml(doc: Element, language: String): Element = {
      val ids = List.newBuilder[String]
      doc
        .select("ndlaembed[data-resource='image']")
        .forEach(embed => {
          val noHideByline = !embed.hasAttr("data-hide-byline")
          if (noHideByline) {
            ids += embed.attr("data-resource_id")
          }
        })
      if (ids.result().isEmpty) {
        return doc
      }
      val images = imageApiClient.getImagesWithIds(ids.result()).getOrElse(List.empty)
      doc
        .select("ndlaembed[data-resource='image']")
        .forEach(embed => {
          val noHideByline = !embed.hasAttr("data-hide-byline")
          if (noHideByline) {
            val imageId = embed.attr("data-resource_id")
            val image   = images.find(i => i.id == imageId)
            embed
              .attr(
                "data-hide-byline",
                s"${image.exists(i => !i.copyright.license.license.equals("COPYRIGHTED"))}"
              ): Unit
          }
        })
      doc
    }
  }
}
