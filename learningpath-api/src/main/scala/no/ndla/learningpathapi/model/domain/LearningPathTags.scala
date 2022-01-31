/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.language.model.WithLanguage

case class LearningPathTags(tags: Seq[String], language: String) extends WithLanguage
