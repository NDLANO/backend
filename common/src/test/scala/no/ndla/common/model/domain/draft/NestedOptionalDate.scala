/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.draft

import no.ndla.common.model.TestObjectWithOptionalDate

case class NestedOptionalDate(subfield: Option[TestObjectWithOptionalDate])
