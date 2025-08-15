/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import java.io.File
import sttp.model.Part
import no.ndla.common.DeriveHelpers

case class FileForm(file: Part[File])

object FileForm {
  import sttp.tapir.Schema
  implicit def schema: Schema[FileForm] = DeriveHelpers.getSchema[FileForm]
}
