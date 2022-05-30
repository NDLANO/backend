/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model

package object domain {
  type FolderData       = Either[Folder, Resource]
  type FeideID          = String
  type FeideAccessToken = String
}
