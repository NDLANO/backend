/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.grep

case class GrepBundle(
    kjerneelementer: List[GrepElement],
    kompetansemaal: List[GrepElement],
    kompetansemaalsett: List[GrepElement],
    tverrfagligeTemaer: List[GrepElement]
) {

  val grepContext: List[GrepElement] = kjerneelementer ++ kompetansemaal ++ kompetansemaalsett ++ tverrfagligeTemaer

  val grepContextByCode: Map[String, GrepElement] =
    Map.from(grepContext.map(elem => elem.kode -> elem))

}
