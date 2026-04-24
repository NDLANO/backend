/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Gets the Authorization header from the incoming request and passes it along to the outgoing request. */
public class AuthorizationRequestInitializer implements ClientHttpRequestInitializer {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationRequestInitializer.class);

    @Override
    public void initialize(@NonNull ClientHttpRequest request) {
        var incomingRequest = RequestContextHolder.getRequestAttributes();
        if (incomingRequest == null) {
            logger.warn("No incoming request found in context, skipping authorization header");
            return;
        }

        var servletRequest = ((ServletRequestAttributes) incomingRequest).getRequest();
        var authHeader = servletRequest.getHeader("Authorization");

        if (authHeader != null && !authHeader.isEmpty()) {
            request.getHeaders().add("Authorization", authHeader);
        }
    }
}
