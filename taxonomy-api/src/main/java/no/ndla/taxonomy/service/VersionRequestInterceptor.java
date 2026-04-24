/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * Interceptor which extracts http header and places a database schema name in the VersionContext object.
 */
@Component
public class VersionRequestInterceptor implements AsyncHandlerInterceptor {

    private final VersionHeaderExtractor versionHeaderExtractor;

    public VersionRequestInterceptor(VersionHeaderExtractor securityDomain) {
        this.versionHeaderExtractor = securityDomain;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getRequestURI().startsWith("/v1/versions")) {
            VersionContext.clear();
        }
        return Optional.of(request)
                .map(versionHeaderExtractor::getVersionSchemaFromHeader)
                .map(this::setVersionContext)
                .orElse(false);
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        VersionContext.clear();
    }

    private boolean setVersionContext(String tenant) {
        VersionContext.setCurrentVersion(tenant);
        return true;
    }
}
