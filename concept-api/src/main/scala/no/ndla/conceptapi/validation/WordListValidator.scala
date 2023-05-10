/*
 * Part of NDLA validation.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.conceptapi.model.domain.ConceptType.{CONCEPT, WORDCLASS}
import no.ndla.conceptapi.model.domain.{ConceptType, WordList}

object WordListValidator {

  private def conceptTypeValidationMessage: Option[ValidationMessage] = {
    Some(
      ValidationMessage(
        "conceptType",
        s"conceptType needs to be of type $WORDCLASS when wordList is defined"
      )
    )
  }

  private def wordListValidationMessage(conceptType: String): Option[ValidationMessage] = {
    Some(
      ValidationMessage(
        "wordList",
        s"wordList field must be defined when conceptType is of type $conceptType"
      )
    )
  }

  def validateWordList(maybeWordList: Option[WordList], conceptType: ConceptType.Value): Option[ValidationMessage] = {
    (maybeWordList, conceptType) match {
      case (None, WORDCLASS)  => wordListValidationMessage(conceptType.toString)
      case (Some(_), CONCEPT) => conceptTypeValidationMessage
      case (_, _)             => None
    }
  }
}
