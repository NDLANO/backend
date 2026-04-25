/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables {@code @Retryable} processing. Ordered with higher precedence (lower value) than the
 * default {@link Ordered#LOWEST_PRECEDENCE} of the {@code @Transactional} advisor so retry sits
 * outside the transaction: a deadlock-rolled-back transaction is fully replayed by the retry
 * advisor, not just the failed statement.
 */
@Configuration
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE - 10)
public class RetryConfig {}
