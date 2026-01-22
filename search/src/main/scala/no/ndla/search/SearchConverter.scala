/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.TagAttribute
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
    parseHtml(html).select(EmbedTagName).asScala.map(embed => getEmbedValuesFromEmbed(embed, language)).toList
  }

  private def getEmbedValuesFromEmbed(embed: Element, language: String): EmbedValues =
    EmbedValues(resource = getEmbedResource(embed), id = getEmbedIds(embed), language = language)

  private def getEmbedResource(embed: Element): Option[String] = {
    embed.attr(TagAttribute.DataResource.toString) match {
      case "" => None
      case a  => Some(a)
    }
  }

  private val AttributesToKeep = List(
    TagAttribute.DataVideoId,
    TagAttribute.DataUrl,
    TagAttribute.DataResource_Id,
    TagAttribute.DataContentId,
    TagAttribute.DataArticleId,
  )

  private def stripIdPostfix(str: String): String = {
    // NOTE: Some video ids can contain data like timestamp (`&t=123`)
    //       Stripping that for better search results
    str.takeWhile(_ != '&')
  }

  // If the string contains an '=' character, return the substring after the last '=';
  private def substringAfterEquals(str: String): String = {
    val idx = str.lastIndexOf('=')
    if (idx >= 0 && idx < str.length - 1) str.substring(idx + 1)
    else str
  }

  private def extractIdFromUrl(resourceUrl: String): List[String] = {
    if (resourceUrl.startsWith("http://") || resourceUrl.startsWith("https://")) {
      resourceUrl.split('/').filter(_.nonEmpty).lastOption match {
        case Some(last) => List(resourceUrl, substringAfterEquals(last))
        case None       => List(resourceUrl)
      }
    } else List(resourceUrl)
  }
  private def getEmbedIds(embed: Element): List[String] = {
    AttributesToKeep.flatMap(attr =>
      embed.attr(attr.toString) match {
        case ""    => None
        case value => extractIdFromUrl(stripIdPostfix(value))
      }
    )
  }

}
