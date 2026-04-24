/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.exceptions;

public class NotFoundServiceException extends RuntimeException {
    public NotFoundServiceException() {}

    public NotFoundServiceException(String type, Object id) {
        super(type + " with id " + id + " not found");
    }

    public NotFoundServiceException(String message) {
        super(message);
    }

    public NotFoundServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundServiceException(Throwable cause) {
        super(cause);
    }
}
