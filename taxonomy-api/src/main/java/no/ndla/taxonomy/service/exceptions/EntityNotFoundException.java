/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.exceptions;

public class EntityNotFoundException extends Exception {
    public EntityNotFoundException(String id) {
        super("Entity not found: " + id);
    }

    public EntityNotFoundException(Integer id) {
        this(id != null ? id.toString() : "null");
    }
}
