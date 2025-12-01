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
import no.ndla.common.model.api.search.ArticleTrait
import no.ndla.common.model.api.search.ArticleTrait.{Audio, Interactive, Podcast, Video}
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

  def getArticleTraits(contents: Seq[ArticleContent]): List[ArticleTrait] = contents
    .flatMap { content =>
      val html      = parseHtml(content.content)
      val embedTags = html.select(EmbedTagName).asScala
      val init      = List.empty[ArticleTrait]
      embedTags.foldLeft(init)((acc, embed) => acc ++ embedToMaybeTrait(embed))
    }
    .toList
    .distinct

  private val videoUrl       = List("youtu", "vimeo", "filmiundervisning", "imdb", "nrk", "khanacademy")
  private val interactiveUrl = List(
    "arcgis.com",
    "arcg.is",
    "geogebra.org",
    "ggbm.at",
    "phet.colorado.edu",
    "3dwarehouse.sketchup.com",
    "lab.concord.org",
    "miljoatlas.miljodirektoratet.no",
    "trinket.io",
    "codepen.io",
  )
  private def embedToMaybeTrait(embed: Element): Option[ArticleTrait] = {
    val dataResource = embed.attr("data-resource")
    val dataUrl      = embed.attr("data-url")
    val dataType     = embed.attr("data-type")
    dataResource match {
      case "brightcove" | "nrk"                                             => Video.some
      case "external" | "iframe" if videoUrl.exists(dataUrl.contains)       => Video.some
      case "external" | "iframe" if interactiveUrl.exists(dataUrl.contains) => Interactive.some
      case "h5p"                                                            => Interactive.some
      case "audio" if dataType == "podcast"                                 => Podcast.some
      case "audio"                                                          => Audio.some
      case _                                                                => None
    }
  }

  def getAttributes(html: String): List[String] = {
    parseHtml(html).select(EmbedTagName).asScala.flatMap(getAttributes).toList
  }

  private def getAttributes(embed: Element): List[String] = {
    val attributesToKeep =
      List("data-title", "data-caption", "data-alt", "data-link-text", "data-edition", "data-publisher", "data-authors")

    attributesToKeep.flatMap(attr =>
      embed.attr(attr) match {
        case "" => None
        case a  => Some(a)
      }
    )
  }

}
