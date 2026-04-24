/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"name"})
public class TranslationPUT {
    @JsonProperty
    @Schema(
            description = "The translated name of the element. Used wherever translated texts are used.",
            example = "Trigonometry")
    public String name;
}
