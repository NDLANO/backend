/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface EntityWithMetadata {
    Metadata getMetadata();

    default void setMetadata(Metadata metadata, boolean changeOwner) {
        setCustomFields(metadata.getCustomFields());
        setGrepCodes(metadata.getGrepCodes());
        setVisible(metadata.isVisible());
        setUpdatedAt(metadata.getUpdatedAt());
        setCreatedAt(metadata.getCreatedAt());
        if (changeOwner) {
            metadata.setParent(this);
        }
    }

    default void setMetadata(Metadata metadata) {
        this.setMetadata(metadata, true);
    }

    Set<JsonGrepCode> getGrepCodes();

    boolean isVisible();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Map<String, String> getCustomFields();

    void setCustomField(String key, String value);

    void unsetCustomField(String key);

    void setGrepCodes(Set<JsonGrepCode> codes);

    void setCustomFields(Map<String, String> customFields);

    void setVisible(boolean visible);

    void setUpdatedAt(Instant updatedAt);

    void setCreatedAt(Instant createdAt);
}
