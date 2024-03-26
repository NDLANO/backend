/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */
package no.ndla.draftapi.model

import no.ndla.common.model.domain.draft.Draft

package object domain {

  def emptySomeToNone(lang: Option[String]): Option[String] = {
    lang.filter(_.nonEmpty)
  }

  type IgnoreFunction = (Option[Draft], StateTransition) => Boolean
}
