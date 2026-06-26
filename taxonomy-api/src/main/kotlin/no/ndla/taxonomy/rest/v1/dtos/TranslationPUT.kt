/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class TranslationPUT(
    @field:Schema(
        description =
            "The translated name of the element. Used wherever translated texts are used.",
        example = "Trigonometry",
    )
    val name: String
)
