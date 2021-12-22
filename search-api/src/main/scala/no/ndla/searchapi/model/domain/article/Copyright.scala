/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import java.util.Date

import no.ndla.searchapi.model.domain.Author

case class Copyright(license: String,
                     origin: String,
                     creators: Seq[Author],
                     processors: Seq[Author],
                     rightsholders: Seq[Author],
                     agreementId: Option[Long],
                     validFrom: Option[Date],
                     validTo: Option[Date])
