/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import sttp.tapir.Schema.annotations.description

@description("Information about the uploaded file")
case class UploadedFile(
    @description("Uploaded file's basename") filename: String,
    @description("Uploaded file's mime type") mime: String,
    @description("Uploaded file's file extension") extension: String,
    @description("Full path of uploaded file") path: String
)
