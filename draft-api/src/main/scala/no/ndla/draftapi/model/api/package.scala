/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model

package object api {
  type RelatedContent = Either[api.RelatedContentLink, Long];
}
