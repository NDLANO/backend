/*
 * Part of NDLA validation
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.conceptapi.model.domain.ConceptType.{CONCEPT, GLOSS}
import no.ndla.conceptapi.model.domain.{ConceptType, GlossData}

object GlossDataValidator {

  private def conceptTypeValidationMessage: Option[ValidationMessage] = {
    Some(
      ValidationMessage(
        "conceptType",
        s"conceptType needs to be of type $GLOSS when glossData is defined"
      )
    )
  }

  private def glossDataValidationMessage(conceptType: String): Option[ValidationMessage] = {
    Some(
      ValidationMessage(
        "glossData",
        s"glossData field must be defined when conceptType is of type $conceptType"
      )
    )
  }

  def validateGlossData(
      maybeGlossData: Option[GlossData],
      conceptType: ConceptType.Value
  ): Option[ValidationMessage] = {
    (maybeGlossData, conceptType) match {
      case (None, GLOSS)      => glossDataValidationMessage(conceptType.toString)
      case (Some(_), CONCEPT) => conceptTypeValidationMessage
      case (_, _)             => None
    }
  }
}
