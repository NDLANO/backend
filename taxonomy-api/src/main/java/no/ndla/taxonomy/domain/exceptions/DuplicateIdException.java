/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateIdException extends RuntimeException {
    public DuplicateIdException(String id) {
        super("Object with id " + id + " already exists");
    }

    public DuplicateIdException() {
        super("Object already exists");
    }
}
