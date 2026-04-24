/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.exceptions;

public class InvalidPublicIdException extends Exception {
    public InvalidPublicIdException(String publicId) {
        super("Invalid publicId " + publicId);
    }
}
