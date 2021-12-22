/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.oembedproxy.service

import io.lemonlabs.uri.Url
import no.ndla.oembedproxy.model.OEmbed
import io.lemonlabs.uri.dsl._
import org.jsoup.Jsoup

object OEmbedConverterService {

  def addYoutubeTimestampIfdefinedInRequest(requestUrl: String, oembed: OEmbed): OEmbed = {
    val paramTypesToTransfer = List("start", "time_continue", "t", "end", "rel")
    val queryParamsToTransfer = requestUrl.query.filterNames(pn => paramTypesToTransfer.contains(pn)).params

    queryParamsToTransfer match {
      case Vector() => oembed
      case params =>
        val newHtml = oembed.html
          .map(Jsoup.parseBodyFragment)
          .map(document => {
            Option(document.select("iframe[src]").first)
              .foreach(element => {
                val newUrl = element.attr("src").addParams(queryParamsToTransfer).toString
                element.attr("src", newUrl.toString)
              })
            document
              .body()
              .html()
              .replaceAll("&amp;", "&") // JSoup escapes & - even in attributes, and there is no way to disable it
          })
        oembed.copy(html = newHtml)
    }
  }

  def handleYoutubeRequestUrl(url: String): String = {
    val filtered = filterQueryNames(url.replaceAll("&amp;", "&"), Set("v"))

    filtered.path.parts.toList match {
      case "embed" :: videoId :: tail => idToYoutubeUrl(videoId)
      case "v" :: videoId :: tail     => idToYoutubeUrl(videoId)
      case _                          => filtered
    }
  }

  def idToYoutubeUrl(videoId: String): String = s"https://youtu.be/$videoId"

  def removeQueryString(url: String): String =
    Url.parse(url).removeQueryString()

  def removeQueryStringAndFragment(url: String): String =
    Url.parse(removeQueryString(url)).withFragment(None)

  private def filterQueryNames(url: String, allowedQueryParamNames: Set[String]): String =
    Url.parse(url).filterQueryNames(allowedQueryParamNames.contains)
}
