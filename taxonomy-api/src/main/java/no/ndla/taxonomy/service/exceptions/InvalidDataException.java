/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.exceptions;

public class InvalidDataException extends Exception {
    public InvalidDataException(String message) {
        super(message);
    }
}
