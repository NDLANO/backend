/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema data class UpdateNotesDTO(val data: Collection<DraftNotesDTO>)
