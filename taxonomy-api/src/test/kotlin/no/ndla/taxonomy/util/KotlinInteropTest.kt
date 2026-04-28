/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinInteropTest {
    @Test
    fun `greets by name`() {
        assertEquals("hello, taxonomy", KotlinInterop.greet("taxonomy"))
    }
}
