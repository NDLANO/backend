/*
 * Part of NDLA common.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import no.ndla.common.model.LanguageType
import sttp.tapir.Schema.annotations.description

case class FakeArticle(
    @description("The title of the resource") title: LanguageType[String, "title"],
    @description("Some description of the resource") description: LanguageType[String, "description"],
    @description("Some introduction of the resource") introduction: LanguageType[String, "introduction"]
)

object FakeArticle {}
