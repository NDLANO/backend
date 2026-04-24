/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import no.ndla.taxonomy.domain.exceptions.*;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
    private String createErrorBody(RuntimeException exception) {
        try {
            final var jsonObject = JsonNodeFactory.instance.objectNode();
            jsonObject.put("error", exception.getMessage());
            return jsonObject.toString();
        } catch (Exception jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    private HttpHeaders createHeaders() {
        final var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    @ExceptionHandler({JWTVerificationException.class, AccessDeniedException.class})
    protected ResponseEntity<String> handleAuthenticationExceptions(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    protected ResponseEntity<String> handleServiceUnavailableException(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({
        ChildNotFoundException.class,
        NotFoundException.class,
        NotFoundServiceException.class,
        NotFoundHttpResponseException.class
    })
    protected ResponseEntity<String> handleNotFoundServiceException(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({DuplicateIdException.class, DuplicateConnectionException.class})
    protected ResponseEntity<String> handleConflictExceptions(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({UnsupportedOperationException.class})
    protected ResponseEntity<String> handleUnsupportedExceptions(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.GONE);
    }

    @ExceptionHandler({
        IdFormatException.class,
        InvalidArgumentServiceException.class,
        IllegalArgumentException.class,
        PrimaryParentRequiredException.class
    })
    protected ResponseEntity<String> handleInvalidArgumentExceptions(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NullPointerException.class, RestClientException.class})
    protected ResponseEntity<String> handleRestOfExceptions(RuntimeException exception) {
        return new ResponseEntity<>(createErrorBody(exception), createHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
