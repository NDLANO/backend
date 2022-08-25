/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

package object domain {
  type RelatedContent = Either[RelatedContentLink, Long]
}
