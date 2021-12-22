/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class BannerImage(mobileUrl: Option[String], mobileId: Option[Long], desktopUrl: String, desktopId: Long)
