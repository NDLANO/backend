/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

// format: off
@description("oEmbed information for an url.")
case class OEmbedDTO(
    @description("The resource type") `type`: String,
    @description("The oEmbed version number. This must be 1.0.") version: String,
    @description("A text title, describing the resource.") title: Option[String],
    @description("A text description, describing the resource. Not standard.") description: Option[String],
    @description("The name of the author/owner of the resource.") authorName: Option[String],
    @description("A URL for the author/owner of the resource.") authorUrl: Option[String],
    @description("The name of the resource provider.") providerName: Option[String],
    @description("The url of the resource provider.") providerUrl: Option[String],
    @description("The suggested cache lifetime for this resource, in seconds. Consumers may choose to use this value or not.") cacheAge: Option[ Long],
    @description("A URL to a thumbnail image representing the resource. The thumbnail must respect any maxwidth and maxheight parameters. If this parameter is present, thumbnail_width and thumbnail_height must also be present.") thumbnailUrl: Option[String],
    @description("The width of the optional thumbnail. If this parameter is present, thumbnail_url and thumbnail_height must also be present.") thumbnailWidth: Option[Long],
    @description("The height of the optional thumbnail. If this parameter is present, thumbnail_url and thumbnail_width must also be present.") thumbnailHeight: Option[Long],
    @description("The source URL of the image. Consumers should be able to insert this URL into an <img> element. Only HTTP and HTTPS URLs are valid. Required if type is photo.") url: Option[String],
    @description("The width in pixels. Required if type is photo/video/rich") width: Option[Long],
    @description("The height in pixels. Required if type is photo/video/rich") height: Option[Long],
    @description("The HTML required to embed a video player. The HTML should have no padding or margins. Consumers may wish to load the HTML in an off-domain iframe to avoid XSS vulnerabilities. Required if type is video/rich.") html: Option[String]
)
// format: on

object OEmbedDTO {
  implicit val encoder: Encoder[OEmbedDTO] = deriveEncoder
  implicit val decoder: Decoder[OEmbedDTO] = deriveDecoder
}
