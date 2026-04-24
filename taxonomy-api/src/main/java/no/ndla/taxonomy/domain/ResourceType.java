/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

@Entity
public class ResourceType extends DomainObject implements Comparable<ResourceType> {

    public ResourceType() {
        setPublicId(URI.create("urn:resourcetype:" + UUID.randomUUID()));
    }

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ResourceType parent;

    @OneToMany(
            mappedBy = "parent",
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST})
    private Set<ResourceType> subtypes = new HashSet<>();

    @Type(JsonBinaryType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    private List<JsonTranslation> translations = new ArrayList<>();

    @Column
    private int order = -1;

    public ResourceType name(String name) {
        setName(name);
        return this;
    }

    public Optional<ResourceType> getParent() {
        return Optional.ofNullable(parent);
    }

    public void setParent(ResourceType parent) {
        final var previousParent = this.parent;

        this.parent = parent;

        if (previousParent != null && previousParent != parent) {
            previousParent.removeSubType(this);
        }

        if (parent != null && !parent.getSubtypes().contains(this)) {
            parent.addSubtype(this);
        }
    }

    public void addSubtype(ResourceType subtype) {
        subtypes.add(subtype);

        if (subtype.getParent().orElse(null) != this) {
            subtype.setParent(this);
        }
    }

    public void removeSubType(ResourceType resourceType) {
        this.subtypes.remove(resourceType);

        if (resourceType.getParent().orElse(null) == this) {
            resourceType.setParent(null);
        }
    }

    public Set<ResourceType> getSubtypes() {
        return this.subtypes.stream().collect(Collectors.toUnmodifiableSet());
    }

    @PreRemove
    void preRemove() {
        setParent(null);
        new HashSet<>(subtypes).forEach(resourceType -> resourceType.setParent(null));
    }

    @Override
    public int compareTo(@NotNull ResourceType o) {
        if (this.order == -1 || o.order == -1) {
            return this.getPublicId().compareTo(o.getPublicId());
        }
        return this.order - o.order;
    }

    @Override
    public List<JsonTranslation> getTranslations() {
        return this.translations;
    }

    @Override
    public void setTranslations(List<JsonTranslation> translations) {
        this.translations = translations;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceType that = (ResourceType) o;
        return this.getPublicId().equals(that.getPublicId());
    }
}
