/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.util

import cats.syntax.option.catsSyntaxOptionId
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.api.search.SearchTrait
import no.ndla.common.model.api.search.SearchTrait.{Audio, H5p, Podcast, Video}
import no.ndla.common.model.domain.ArticleContent
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.jdk.CollectionConverters.*

class TraitUtil {
  private def parseHtml(html: String): Element = {
    val document = Jsoup.parseBodyFragment(html)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.body()
  }

  def getArticleTraits(contents: Seq[ArticleContent]): List[SearchTrait] =
    contents
      .flatMap { content =>
        val html      = parseHtml(content.content)
        val embedTags = html.select(EmbedTagName).asScala
        val init      = List.empty[SearchTrait]
        embedTags.foldLeft(init)((acc, embed) => acc ++ embedToMaybeTrait(embed))
      }
      .toList
      .distinct

  private val videoUrl = List("youtu", "vimeo", "filmiundervisning", "imdb", "nrk", "khanacademy")
  private def embedToMaybeTrait(embed: Element): Option[SearchTrait] = {
    val dataResource = embed.attr("data-resource")
    val dataUrl      = embed.attr("data-url")
    val dataType     = embed.attr("data-type")
    dataResource match {
      case "h5p"                                                      => H5p.some
      case "brightcove" | "nrk"                                       => Video.some
      case "external" | "iframe" if videoUrl.exists(dataUrl.contains) => Video.some
      case "audio" if dataType == "podcast"                           => Podcast.some
      case "audio"                                                    => Audio.some
      case _                                                          => None
    }
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
