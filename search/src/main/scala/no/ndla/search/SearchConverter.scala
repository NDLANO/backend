/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import cats.implicits.catsSyntaxOptionId
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.search.model.domain.EmbedValues
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.jdk.CollectionConverters.CollectionHasAsScala

object SearchConverter {
  private def parseHtml(html: String) = {
    val document = Jsoup.parseBodyFragment(html)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.body()
  }

  def getEmbedValues(html: String, language: String): List[EmbedValues] = {
    parseHtml(html)
      .select(EmbedTagName)
      .asScala
      .map(embed => getEmbedValuesFromEmbed(embed, language))
      .toList
  }

  private def getEmbedValuesFromEmbed(embed: Element, language: String): EmbedValues =
    EmbedValues(
      resource = getEmbedResource(embed),
      id = getEmbedIds(embed),
      language = language
    )

  private def getEmbedResource(embed: Element): Option[String] = {
    embed.attr("data-resource") match {
      case "" => None
      case a  => Some(a)
    }
  }

  private val AttributesToKeep = List(
    "data-videoid",
    "data-url",
    "data-resource_id",
    "data-content-id",
    "data-article-id"
  )

  private def stripIdPostfix(str: String): String = {
    // NOTE: Some video ids can contain data like timestamp (`&t=123`)
    //       Stripping that for better search results
    str.takeWhile(_ != '&')
  }

  private def getEmbedIds(embed: Element): List[String] = {
    AttributesToKeep.flatMap(attr =>
      embed.attr(attr) match {
        case ""    => None
        case value => stripIdPostfix(value).some
      }
    )
  }

}
