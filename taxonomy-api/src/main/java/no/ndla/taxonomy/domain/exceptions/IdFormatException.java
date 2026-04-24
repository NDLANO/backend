/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain.exceptions;

public class IdFormatException extends RuntimeException {
    public IdFormatException(String message) {
        super(message);
    }
}
