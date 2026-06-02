/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI

@Schema
data class ResolvedUrl(
    @field:Schema(
        description = "ID of the element referred to by the given path",
        example = "urn:resource:1",
    )
    val id: URI,
    @field:Schema(
        description =
            "The ID of this element in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                "for the system, and <id> is the id of this content in that system.",
        example = "urn:article:1",
    )
    // TODO: contentUri should be required per the API spec, but our tests (and maybe db data) lacks
    // it.
    //       Investigate whether missing contentUri represents a data quality issue and enforce
    // non-null if so.
    val contentUri: URI?,
    @field:Schema(
        description =
            "Element name. For performance reasons, this name is for informational purposes only. To get a translated name, please fetch the resolved resource using its rest resource.",
        example = "Basic physics",
        required = true,
    )
    val name: String,
    @field:Schema(
        description =
            "Parent elements of the resolved element. The first element is the parent, the second is the grandparent, etc.")
    val parents: List<URI>,
    @field:Schema(
        description = "URL path for resource",
        example = "'/subject:1/topic:12/resource:12'",
    )
    val path: String,
    @field:Schema(
        description = "Pretty url resource",
        example = "'/r/subject-name/resource-name/hash'",
    )
    val url: String,
    @field:Schema(
        description =
            "Is this an exact match for the provided path? False if this is another path to the same resource.")
    val exactMatch: Boolean,
)
