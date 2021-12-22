/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.grep

case class GrepBundle(
    kjerneelementer: List[GrepElement],
    kompetansemaal: List[GrepElement],
    tverrfagligeTemaer: List[GrepElement],
)
