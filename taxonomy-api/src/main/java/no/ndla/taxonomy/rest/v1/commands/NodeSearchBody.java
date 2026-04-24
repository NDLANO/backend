/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.NodeType;

@Schema(name = "NodeSearchBody")
public class NodeSearchBody {
    @JsonProperty
    @Schema(
            description =
                    "If specified, the search result will be filtered by whether they include the key,value combination provided. If more than one provided only one will be required (OR)")
    public Optional<Map<String, String>> customFields = Optional.empty();

    @Schema(description = "ISO-639-1 language code", example = "nb")
    @JsonProperty
    public String language = Constants.DefaultLanguage;

    @Schema(description = "How many results to return per page")
    @JsonProperty
    public int pageSize = 10;

    @Schema(description = "Which page to fetch")
    @JsonProperty
    public int page = 1;

    @Schema(description = "Query to search names")
    @JsonProperty
    public Optional<String> query = Optional.empty();

    @Schema(description = "Ids to fetch for query")
    @JsonProperty
    public Optional<List<String>> ids = Optional.empty();

    @Schema(description = "ContentURIs to fetch for query")
    @JsonProperty
    public Optional<List<String>> contentUris = Optional.empty();

    @Schema(description = "Filter by nodeType")
    @JsonProperty
    public Optional<List<NodeType>> nodeType = Optional.empty();

    @Schema(description = "Include all contexts")
    @JsonProperty
    public boolean includeContexts = false;

    @Schema(description = "Filter out programme contexts")
    @JsonProperty
    public boolean filterProgrammes = false;

    @Schema(description = "Id to root id in context.")
    @JsonProperty
    public Optional<URI> rootId = Optional.empty();

    @Schema(description = "Id to parent id in context.")
    @JsonProperty
    public Optional<URI> parentId = Optional.empty();

    public Optional<Map<String, String>> getCustomFields() {
        return customFields;
    }
}
