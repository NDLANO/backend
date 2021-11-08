/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

case class SearchableTitles(nb: Option[String],
                            nn: Option[String],
                            en: Option[String],
                            fr: Option[String],
                            de: Option[String],
                            es: Option[String],
                            se: Option[String],
                            zh: Option[String],
                            unknown: Option[String])
