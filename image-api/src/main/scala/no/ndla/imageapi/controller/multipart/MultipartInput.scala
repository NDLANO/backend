/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller.multipart

import no.ndla.imageapi.model.api.{NewImageMetaInformationV2DTO, UpdateImageMetaInformationDTO}
import sttp.model.Part

import java.io.File

case class MetaDataAndFileForm(metadata: Part[NewImageMetaInformationV2DTO], file: Part[File])

case class CopyMetaDataAndFileForm(file: Part[File])

case class UpdateMetaDataAndFileForm(metadata: Part[UpdateImageMetaInformationDTO], file: Option[Part[File]])
