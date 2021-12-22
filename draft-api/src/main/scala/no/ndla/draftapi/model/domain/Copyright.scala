/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import java.util.Date

case class Copyright(license: Option[String],
                     origin: Option[String],
                     creators: Seq[Author],
                     processors: Seq[Author],
                     rightsholders: Seq[Author],
                     agreementId: Option[Long],
                     validFrom: Option[Date],
                     validTo: Option[Date])
