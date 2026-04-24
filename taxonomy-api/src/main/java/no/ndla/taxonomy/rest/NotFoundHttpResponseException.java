/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest;

public class NotFoundHttpResponseException extends RuntimeException {
    public NotFoundHttpResponseException(String message) {
        super(message);
    }
}
