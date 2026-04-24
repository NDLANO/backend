/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Translation;

@Schema(
        name = "Translation",
        requiredProperties = {"language", "name"})
public class TranslationDTO implements Comparable<TranslationDTO> {
    public TranslationDTO() {}

    public TranslationDTO(String language, String name) {
        this.language = language;
        this.name = name;
    }

    public TranslationDTO(Translation translation) {
        name = translation.getName();
        language = translation.getLanguageCode();
    }

    @JsonProperty
    @Schema(description = "The translated name of the node", example = "Trigonometry")
    public String name;

    @JsonProperty
    @Schema(description = "ISO 639-1 language code", example = "en")
    public String language;

    @Override
    public int compareTo(TranslationDTO o) {
        return language.compareTo(o.language);
    }
}
