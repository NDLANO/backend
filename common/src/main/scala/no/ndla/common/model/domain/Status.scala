/*
 * Part of NDLA common.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import no.ndla.common.model.domain.draft.DraftStatus

case class Status(current: DraftStatus, other: Set[DraftStatus])
