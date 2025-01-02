package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.Props
import no.ndla.articleapi.db.HtmlMigration
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.network.NdlaClient
import org.jsoup.nodes.Element

trait V55__SetHideBylineForImagesNotCopyrighted {
  this: ImageApiClient & NdlaClient & Props =>

  class V55__SetHideBylineForImagesNotCopyrighted extends HtmlMigration {
    override val tableName: String  = "contentdata"
    override val columnName: String = "document"

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
          val imageId = embed.attr("data-resource_id")
          val image   = images.find(i => i.id == imageId)
          embed
            .attr("data-hide-byline", s"${image.exists(i => !i.copyright.license.license.equals("COPYRIGHTED"))}"): Unit
        })
      doc
    }
  }

}
