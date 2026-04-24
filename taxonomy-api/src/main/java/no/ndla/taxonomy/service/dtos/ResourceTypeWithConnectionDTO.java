/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Translation;

@Schema(
        name = "ResourceTypeWithConnection",
        requiredProperties = {"id", "name", "connectionId", "translations", "supportedLanguages"})
public class ResourceTypeWithConnectionDTO implements Comparable<ResourceTypeWithConnectionDTO> {
    @JsonProperty
    @Schema(example = "urn:resourcetype:2")
    private URI id;

    @JsonProperty
    @Schema(description = "Internal order of the resource types")
    private int order;

    @JsonProperty
    @Schema(example = "urn:resourcetype:1")
    private Optional<URI> parentId = Optional.empty();

    @JsonProperty
    @Schema(description = "The name of the resource type", example = "Lecture")
    private String name;

    @JsonProperty
    @Schema(description = "All translations of this resource type")
    private TreeSet<TranslationDTO> translations;

    @JsonProperty
    @Schema(description = "List of language codes supported by translations")
    private TreeSet<String> supportedLanguages;

    @JsonProperty
    @Schema(description = "The id of the resource resource type connection", example = "urn:resource-resourcetype:1")
    private URI connectionId;

    public ResourceTypeWithConnectionDTO() {}

    public ResourceTypeWithConnectionDTO(ResourceResourceType resourceResourceType, String languageCode) {
        var resourceType = resourceResourceType.getResourceType();

        this.id = resourceType.getPublicId();
        this.order = resourceType.getOrder();

        var translations = resourceType.getTranslations();
        this.translations =
                translations.stream().map(TranslationDTO::new).collect(Collectors.toCollection(TreeSet::new));
        this.supportedLanguages =
                this.translations.stream().map(t -> t.language).collect(Collectors.toCollection(TreeSet::new));

        this.parentId = resourceType.getParent().map(ResourceType::getPublicId);

        this.name = translations.stream()
                .filter(t -> Objects.equals(t.getLanguageCode(), languageCode))
                .findFirst()
                .map(Translation::getName)
                .orElse(resourceType.getName());

        this.connectionId = resourceResourceType.getPublicId();
    }

    public URI getId() {
        return id;
    }

    public Optional<URI> getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public Set<TranslationDTO> getTranslations() {
        return translations;
    }

    public URI getConnectionId() {
        return connectionId;
    }

    @Override
    public int compareTo(ResourceTypeWithConnectionDTO other) {
        if (this.order == -1 || other.order == -1) {
            return this.id.compareTo(other.id);
        }
        return this.order - other.order;
    }
}
