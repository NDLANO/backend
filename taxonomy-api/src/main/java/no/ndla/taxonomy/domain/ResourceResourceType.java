/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.*;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@Entity
public class ResourceResourceType extends DomainEntity implements Comparable<ResourceResourceType> {

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "resource_id")
    private Node node;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    public ResourceResourceType() {
        setPublicId(URI.create("urn:resource-resourcetype:" + UUID.randomUUID()));
    }

    public static ResourceResourceType create(Node resource, ResourceType resourceType) {
        final var resourceResourceType = new ResourceResourceType();

        resourceResourceType.node = resource;
        resourceResourceType.resourceType = resourceType;

        resource.addResourceResourceType(resourceResourceType);

        return resourceResourceType;
    }

    public void disassociate() {
        final var resource = this.node;
        final var resourceType = this.resourceType;

        this.resourceType = null;
        this.node = null;

        if (resource != null) {
            resource.removeResourceResourceType(this);
        }
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node resource) {
        this.node = resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @PreRemove
    void preRemove() {
        this.disassociate();
    }

    @Override
    public int compareTo(ResourceResourceType other) {
        if (this.resourceType == null || other.resourceType == null) {
            return 0;
        }
        return this.getResourceType().compareTo(other.getResourceType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceResourceType that = (ResourceResourceType) o;
        return Objects.equals(node.getPublicId(), that.node.getPublicId())
                && Objects.equals(resourceType.getPublicId(), that.resourceType.getPublicId());
    }
}
