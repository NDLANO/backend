/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import no.ndla.common.model.domain.draft.DraftStatus

case class SearchableStatus(current: DraftStatus.Value, other: Set[DraftStatus.Value])
