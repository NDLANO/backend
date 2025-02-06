/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller.multipart

import no.ndla.audioapi.model.api.{NewAudioMetaInformationDTO, UpdatedAudioMetaInformationDTO}
import sttp.model.Part

import java.io.File

case class MetaDataAndFileForm(
    metadata: Part[NewAudioMetaInformationDTO],
    file: Part[File]
)
case class MetaDataAndOptFileForm(
    metadata: Part[UpdatedAudioMetaInformationDTO],
    file: Option[Part[File]]
)
