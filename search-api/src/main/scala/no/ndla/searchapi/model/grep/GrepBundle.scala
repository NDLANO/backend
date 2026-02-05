/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.grep

case class GrepBundle(
    kjerneelementer: List[GrepKjerneelement],
    kompetansemaal: List[GrepKompetansemaal],
    kompetansemaalsett: List[GrepKompetansemaalSett],
    tverrfagligeTemaer: List[GrepElement],
    laereplaner: List[GrepLaererplan],
    fagkoder: List[GrepFagkode],
) {

  val grepContext: List[GrepElement] = kjerneelementer ++
    kompetansemaal ++
    kompetansemaalsett ++
    tverrfagligeTemaer ++
    laereplaner ++
    fagkoder

  val grepContextByCode: Map[String, GrepElement] = Map.from(grepContext.map(elem => elem.kode -> elem))

}
