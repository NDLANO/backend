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
import sttp.tapir.generic.auto.*
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.json.circe.*

import java.io.File

case class MetaDataAndFileForm(
    metadata: NewAudioMetaInformationDTO,
    file: Part[File]
)
object MetaDataAndFileForm {
  implicit val codec: JsonCodec[NewAudioMetaInformationDTO] = circeCodec[NewAudioMetaInformationDTO]
}

case class MetaDataAndOptFileForm(
    metadata: UpdatedAudioMetaInformationDTO,
    file: Option[Part[File]]
)
object MetaDataAndOptFileForm {
  implicit val codec: JsonCodec[UpdatedAudioMetaInformationDTO] = circeCodec[UpdatedAudioMetaInformationDTO]
}
