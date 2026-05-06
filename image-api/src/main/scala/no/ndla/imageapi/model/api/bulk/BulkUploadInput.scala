/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api.bulk

import no.ndla.common.model.domain.UploadedFile
import no.ndla.imageapi.model.api.NewImageMetaInformationV2DTO

case class BulkUploadInput(metadata: NewImageMetaInformationV2DTO, file: UploadedFile)
