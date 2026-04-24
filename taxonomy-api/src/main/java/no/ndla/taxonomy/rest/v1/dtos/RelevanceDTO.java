/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.dtos.TranslationDTO;

@Schema(
        name = "Relevance",
        requiredProperties = {"id", "name", "translations", "supportedLanguages"})
public class RelevanceDTO {
    @JsonProperty
    @Schema(description = "Specifies if node is core or supplementary", example = "urn:relevance:core")
    public URI id;

    @JsonProperty
    @Schema(description = "The name of the relevance", example = "Core")
    public String name;

    @JsonProperty
    @Schema(description = "All translations of this relevance")
    private Set<TranslationDTO> translations;

    @JsonProperty
    @Schema(description = "List of language codes supported by translations")
    private Set<String> supportedLanguages;

    /** Need a default constructor for Jackson */
    public RelevanceDTO() {}

    public RelevanceDTO(Relevance relevance) {
        this.id = relevance.getPublicId();
        this.name = relevance.getTranslatedName();
        this.translations =
                relevance.getTranslations().stream().map(TranslationDTO::new).collect(Collectors.toSet());
        this.supportedLanguages =
                this.translations.stream().map(t -> t.language).collect(Collectors.toSet());
    }

    public RelevanceDTO(URI publicId, String name, Set<TranslationDTO> translations) {
        this.id = publicId;
        this.name = name;
        this.translations = translations;
        this.supportedLanguages =
                this.translations.stream().map(t -> t.language).collect(Collectors.toSet());
    }

    public RelevanceDTO getTranslated(String language) {
        var translatedName = translations.stream()
                .filter(t -> Objects.equals(t.language, language))
                .findFirst()
                .map(t -> t.name)
                .orElse(this.name);

        return new RelevanceDTO(this.id, translatedName, this.translations);
    }

    public Set<TranslationDTO> getTranslations() {
        return translations;
    }

    public Optional<TranslationDTO> getTranslation(String language) {
        return translations.stream()
                .filter(t -> Objects.equals(t.language, language))
                .findFirst();
    }
}
