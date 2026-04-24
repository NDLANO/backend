/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.ndla.taxonomy.util.HashUtil;

@Entity
public class Version extends DomainEntity {
    @Column
    @Enumerated(EnumType.STRING)
    private VersionType versionType = VersionType.BETA;

    @Column
    private String name;

    @Column
    private final String hash;

    private boolean locked = false;

    @Column
    private Instant created;

    @Column
    private Instant published;

    @Column
    private Instant archived;

    public Version() {
        setPublicId(URI.create("urn:version:" + UUID.randomUUID()));
        this.hash = HashUtil.shortHash(getPublicId());
        this.created = Instant.now();
    }

    public VersionType getVersionType() {
        return versionType;
    }

    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getPublished() {
        return published;
    }

    public void setPublished(Instant published) {
        this.published = published;
    }

    public Instant getArchived() {
        return archived;
    }

    public void setArchived(Instant archived) {
        this.archived = archived;
    }
}
