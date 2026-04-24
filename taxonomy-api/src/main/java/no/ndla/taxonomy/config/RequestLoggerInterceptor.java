/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class RequestLoggerInterceptor implements AsyncHandlerInterceptor {
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    static final String START_TIME = "startTime";

    public String getCorrelationId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Correlation-ID")).orElse("");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        MDC.put("Correlation-ID", getCorrelationId(request));
        var requestPath = request.getRequestURI();
        var queryString = Optional.ofNullable(request.getQueryString()).orElse("");
        var pathAndQuery = requestPath + (queryString.isEmpty() ? "" : "?" + queryString);
        MDC.put("requestPath", pathAndQuery);
        MDC.put("method", request.getMethod());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        long startTime = (Long) request.getAttribute(START_TIME);
        long latency = System.currentTimeMillis() - startTime;
        MDC.put("reqLatencyMs", Long.toString(latency));
        logger.info(
                "({}) {} {}?{} ({}) executed in {}ms with code {}",
                Optional.ofNullable(request.getHeader("X-Correlation-ID")).orElse(""),
                request.getMethod(),
                request.getRequestURI(),
                Optional.ofNullable(request.getQueryString()).orElse(""),
                Optional.ofNullable(request.getHeader("VersionHash")).orElse(""),
                "" + latency,
                response.getStatus());
        AsyncHandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
