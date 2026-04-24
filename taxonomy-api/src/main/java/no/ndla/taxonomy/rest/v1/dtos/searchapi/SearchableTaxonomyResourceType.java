/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.searchapi;

import static no.ndla.taxonomy.config.Constants.DefaultLanguage;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Optional;
import no.ndla.taxonomy.domain.ResourceType;

@Schema(requiredProperties = {"id", "name"})
public class SearchableTaxonomyResourceType implements Comparable<SearchableTaxonomyResourceType> {
    @JsonProperty
    private String id;

    @JsonProperty
    private Optional<String> parentId;

    @JsonProperty
    private HashMap<String, String> name;

    @JsonProperty
    private int order;

    public SearchableTaxonomyResourceType() {}

    public SearchableTaxonomyResourceType(ResourceType resourceType) {
        var translations = resourceType.getTranslations();
        this.id = resourceType.getPublicId().toString();
        this.order = resourceType.getOrder();
        this.parentId = resourceType.getParent().map(rt -> rt.getPublicId().toString());
        this.name = new HashMap<>();
        this.name.put(DefaultLanguage, resourceType.getName());

        for (var t : translations) {
            name.put(t.getLanguageCode(), t.getName());
        }
    }

    @Override
    public int compareTo(SearchableTaxonomyResourceType other) {
        if (this.order == -1 || other.order == -1) {
            return this.id.compareTo(other.id);
        }
        return this.order - other.order;
    }
}
