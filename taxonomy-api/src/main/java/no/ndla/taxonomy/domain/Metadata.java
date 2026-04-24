/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import no.ndla.taxonomy.rest.v1.dtos.MetadataPUT;

public class Metadata implements Serializable {
    private Instant updatedAt;

    private Instant createdAt;

    private Set<JsonGrepCode> grepCodes = new HashSet<>();

    private Map<String, String> customFields = new HashMap<>();

    private boolean visible = true;

    private EntityWithMetadata parent;

    public Metadata() {}

    public Metadata(EntityWithMetadata parent) {
        this.parent = parent;
        this.grepCodes = parent.getGrepCodes();
        this.visible = parent.isVisible();
        this.createdAt = parent.getCreatedAt();
        this.updatedAt = parent.getUpdatedAt();
        this.customFields = parent.getCustomFields();
    }

    public Metadata(Metadata metadata) {
        this.parent = metadata.parent;
        this.createdAt = metadata.createdAt;
        this.customFields = metadata.getCustomFields();
        this.grepCodes = new HashSet<>(metadata.getGrepCodes());
        this.updatedAt = metadata.updatedAt;
        this.visible = metadata.isVisible();
    }

    public Metadata mergeWith(MetadataPUT toMerge) {
        toMerge.visible.ifPresent(this::setVisible);

        toMerge.grepCodes.ifPresent(this::setGrepCodes);

        toMerge.customFields.ifPresent(this::setCustomFields);

        return this;
    }

    public void setParent(EntityWithMetadata parent) {
        this.parent = parent;
    }

    public void addGrepCode(JsonGrepCode grepCode) {
        this.grepCodes.add(grepCode);
        this.parent.setGrepCodes(this.grepCodes);
    }

    public void setGrepCodes(Set<String> grepCodes) {
        var newJsonGrepCodes = grepCodes.stream().map(JsonGrepCode::new).collect(Collectors.toSet());
        this.grepCodes = newJsonGrepCodes;
        this.parent.setGrepCodes(newJsonGrepCodes);
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
        this.parent.setCustomFields(customFields);
    }

    public void removeGrepCode(JsonGrepCode grepCode) {
        this.grepCodes.remove(grepCode);
        this.parent.setGrepCodes(this.grepCodes);
    }

    public Set<JsonGrepCode> getGrepCodes() {
        return new HashSet<>(grepCodes);
    }

    public Map<String, String> getCustomFields() {
        return this.customFields;
    }

    public boolean isVisible() {
        return visible;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.parent.setVisible(visible);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata that = (Metadata) o;
        return visible == that.visible
                && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(grepCodes, that.grepCodes)
                && Objects.equals(customFields, that.customFields);
    }
}
