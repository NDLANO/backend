/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;

@Schema(
        name = "Version",
        requiredProperties = {"id", "versionType", "name", "hash", "locked", "created"})
public class VersionDTO {
    @JsonProperty
    @Schema(example = "urn:version:1")
    private URI id;

    @JsonProperty
    @Schema(example = "BETA")
    @Enumerated(EnumType.STRING)
    private VersionType versionType;

    @JsonProperty
    @Schema(description = "Name for the version")
    private String name;

    @JsonProperty
    @Schema(description = "Unique hash for the version")
    private String hash;

    @JsonProperty
    @Schema(description = "Is the version locked")
    private Boolean locked;

    @JsonProperty
    @Schema(description = "Timestamp for when version was created")
    private Instant created;

    @JsonProperty
    @Schema(description = "Timestamp for when version was published")
    private Optional<Instant> published = Optional.empty();

    @JsonProperty
    @Schema(description = "Timestamp for when version was archived")
    private Optional<Instant> archived = Optional.empty();

    public VersionDTO() {}

    public VersionDTO(Version version) {
        this.id = version.getPublicId();
        this.versionType = version.getVersionType();
        this.name = version.getName();
        this.hash = version.getHash();
        this.locked = version.isLocked();
        this.created = version.getCreated();
        this.published = Optional.ofNullable(version.getPublished());
        this.archived = Optional.ofNullable(version.getArchived());
    }

    public URI getId() {
        return id;
    }

    public VersionType getVersionType() {
        return versionType;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public Instant getCreated() {
        return created;
    }

    public Optional<Instant> getPublished() {
        return published;
    }

    public Optional<Instant> getArchived() {
        return archived;
    }
}
