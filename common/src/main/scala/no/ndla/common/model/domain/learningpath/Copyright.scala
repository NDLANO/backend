/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.learningpath

import no.ndla.common.model.domain.Author

case class Copyright(license: String, contributors: Seq[Author])
