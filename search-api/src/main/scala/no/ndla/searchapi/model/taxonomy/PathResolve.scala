/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class PathResolve(id: String, contentUri: Option[String], name: String, parents: Seq[String], path: String)
