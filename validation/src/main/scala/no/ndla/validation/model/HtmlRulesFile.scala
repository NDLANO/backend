/*
 * Part of NDLA validation
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation.model

import no.ndla.validation.TagAttribute

case class Field(name: TagAttribute)

case class HtmlRulesAttribute(
    fields: List[Field],
    mustContainAtLeastOneOptionalAttribute: Boolean = false
)

case class HtmlRulesFile(
    attributes: Map[String, HtmlRulesAttribute],
    tags: List[String]
)
