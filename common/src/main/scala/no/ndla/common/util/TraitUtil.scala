/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.util

import no.ndla.common.model.domain.ArticleContent
import no.ndla.common.model.api.search.SearchTrait
import scala.collection.mutable.ListBuffer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import no.ndla.common.configuration.Constants.EmbedTagName
import scala.jdk.CollectionConverters.*

class TraitUtil {
  private def parseHtml(html: String): Element = {
    val document = Jsoup.parseBodyFragment(html)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.body()
  }

  def getArticleTraits(contents: Seq[ArticleContent]): List[SearchTrait] = {
    contents
      .flatMap(content => {
        val traits = ListBuffer[SearchTrait]()
        parseHtml(content.content)
          .select(EmbedTagName)
          .forEach(embed => {
            val dataResource = embed.attr("data-resource")
            dataResource match {
              case "h5p"                 => traits += SearchTrait.H5p
              case "brightcove" | "nrk"  => traits += SearchTrait.Video
              case "external" | "iframe" =>
                val dataUrl = embed.attr("data-url")
                if (
                  dataUrl.contains("youtu") || dataUrl.contains("vimeo") || dataUrl
                    .contains("filmiundervisning") || dataUrl.contains("imdb") || dataUrl
                    .contains("nrk") || dataUrl.contains("khanacademy")
                ) {
                  traits += SearchTrait.Video
                }
              case "audio" =>
                val dataType = embed.attr("data-type")
                dataType match {
                  case "podcast" => traits += SearchTrait.Podcast
                  case _         => traits += SearchTrait.Audio
                }
              case _ => // Do nothing
            }
          })
        traits
      })
      .toList
      .distinct
  }

  def getAttributes(html: String): List[String] = {
    parseHtml(html)
      .select(EmbedTagName)
      .asScala
      .flatMap(getAttributes)
      .toList
  }

  private def getAttributes(embed: Element): List[String] = {
    val attributesToKeep = List(
      "data-title",
      "data-caption",
      "data-alt",
      "data-link-text",
      "data-edition",
      "data-publisher",
      "data-authors"
    )

    attributesToKeep.flatMap(attr =>
      embed.attr(attr) match {
        case "" => None
        case a  => Some(a)
      }
    )
  }

}
