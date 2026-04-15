/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

trait TestEnvironment {
  given props: TestProps = TestProps()
}
