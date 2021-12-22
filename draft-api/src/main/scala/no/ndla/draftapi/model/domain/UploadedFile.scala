/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

case class UploadedFile(fileName: String, filePath: String, size: Long, contentType: String, fileExtension: String)
