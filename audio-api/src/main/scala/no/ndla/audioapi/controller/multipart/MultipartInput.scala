/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.controller.multipart

import no.ndla.audioapi.model.api.{NewAudioMetaInformation, UpdatedAudioMetaInformation}
import sttp.model.Part

import java.io.File

case class MetaDataAndFileForm(
    metadata: Part[NewAudioMetaInformation],
    file: Part[File]
)
case class MetaDataAndOptFileForm(
    metadata: Part[UpdatedAudioMetaInformation],
    file: Option[Part[File]]
)
