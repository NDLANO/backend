/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;

@Entity
@Table(name = "url_map")
public class UrlMapping {
    @Id
    @Column
    private String oldUrl;

    @Column
    private String public_id;

    @Column
    private String subject_id;

    public String getOldUrl() {
        return oldUrl;
    }

    public void setOldUrl(String oldUrl) {
        this.oldUrl = oldUrl;
    }

    public URI getPublic_id() {
        return URI.create(public_id);
    }

    public void setPublic_id(URI public_id) {
        this.public_id = public_id.toString();
    }

    public void setPublic_id(String public_id) {
        this.public_id = URI.create(public_id).toString(); // if not valid URI - will force an IllegalArgumentException
    }

    public URI getSubject_id() {
        return subject_id != null ? URI.create(subject_id) : null;
    }

    public void setSubject_id(URI subject_id) {
        this.subject_id = subject_id.toString();
    }

    public void setSubject_id(String subject_id) {
        this.subject_id = URI.create(subject_id).toString(); // if not valid URI - will force an
        // IllegalArgumentException
    }
}
