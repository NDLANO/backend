/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import no.ndla.taxonomy.domain.SortableResourceConnection;

public class ResourceTreeSortable implements TreeSorter.Sortable {
    private final int rank;
    private final URI id;
    private final URI parentId;
    private SortableResourceConnection resourceConnection;
    private final String parentType;
    private final String type;

    public ResourceTreeSortable(SortableResourceConnection resourceConnection) {
        this.id = resourceConnection
                .getResource()
                .orElseThrow(() -> new IllegalArgumentException("Resource not set"))
                .getPublicId();
        this.parentId = resourceConnection
                .getParent()
                .orElseThrow(() -> new IllegalArgumentException("Parent not set"))
                .getPublicId();
        this.rank = resourceConnection.getRank();
        this.resourceConnection = resourceConnection;
        this.type = "resource";
        this.parentType = "node";
    }

    public ResourceTreeSortable(String type, String parentType, URI id, URI parentId, int rank) {
        this.id = id;
        this.parentId = parentId;
        this.rank = rank;
        this.type = type;
        this.parentType = parentType;
    }

    @Override
    public int getSortableRank() {
        // Sort subject-topic relations first, then topic-resource, then topic-topic

        if ("resource".equals(type)) {
            return rank - 10000;
        }

        if ("topic".equals(type) && "topic".equals(parentType)) {
            return rank - 1000;
        }

        if ("node".equals(type) && "node".equals(parentType)) {
            return rank - 1000;
        }

        return rank;
    }

    @Override
    public URI getSortableId() {
        try {
            return new URI("urn:" + this.type + ":" + this.id);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public URI getSortableParentId() {
        try {
            return new URI("urn:" + this.parentType + ":" + this.parentId);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Optional<SortableResourceConnection> getResourceConnection() {
        return Optional.ofNullable(resourceConnection);
    }
}
