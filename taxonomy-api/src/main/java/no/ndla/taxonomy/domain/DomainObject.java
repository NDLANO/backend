/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class DomainObject extends DomainEntity implements Translatable {
    @Column
    private String name;

    public String getName() {
        return name;
    }

    public String getTranslatedName(String languageCode) {
        return getTranslation(languageCode).map(JsonTranslation::getName).orElse(getName());
    }

    public void setName(String name) {
        this.name = name;
    }
}
