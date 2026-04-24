/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;

@Schema(
        name = "NodeWithParents",
        requiredProperties = {"parents"})
public class NodeWithParents extends NodeDTO {
    @JsonProperty
    @Schema(
            description = "Parent topology nodes and whether or not connection type is primary",
            example = "["
                    + "{\"id\": \"urn:topic:1:181900\"," + "\"name\": \"I dyrehagen\","
                    + "\"contentUri\": \"urn:article:6662\"," + "\"path\": \"/subject:2/topic:1:181900\","
                    + "\"primary\": \"true\"}]")
    private final Set<NodeChildDTO> parents = new HashSet<>();

    public NodeWithParents() {}

    public NodeWithParents(Node node, String languageCode, boolean includeContexts) {
        super(
                Optional.empty(),
                Optional.empty(),
                node,
                NodeConnectionType.BRANCH,
                languageCode,
                Optional.empty(),
                includeContexts,
                false,
                true,
                false);

        node.getParentConnections().stream()
                .map(nodeResource -> {
                    Node parent = nodeResource.getParent().orElseThrow(() -> new NotFoundException("Parent not found"));
                    return new NodeChildDTO(parent, nodeResource, languageCode);
                })
                .forEach(parents::add);
    }

    public Set<NodeChildDTO> getParents() {
        return parents;
    }
}
