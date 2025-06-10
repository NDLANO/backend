/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import com.sksamuel.elastic4s.ElasticApi.{nestedAggregation, termsAgg}
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import no.ndla.testbase.UnitTestSuiteBase

class AggregationBuilderTest extends UnitTestSuiteBase {

  val testMapping: MappingDefinition = {
    val fields = List(
      ObjectField("domainObject", enabled = Some(false)),
      intField("id"),
      keywordField("draftStatus.current"),
      keywordField("draftStatus.other"),
      dateField("lastUpdated"),
      keywordField("license"),
      keywordField("defaultTitle"),
      textField("authors"),
      keywordField("articleType"),
      keywordField("supportedLanguages"),
      textField("notes"),
      textField("previousVersionsNotes"),
      keywordField("users"),
      keywordField("grepContexts.code"),
      textField("grepContexts.title"),
      keywordField("traits"),
      ObjectField(
        "responsible",
        properties = Seq(
          keywordField("responsibleId"),
          dateField("lastUpdated")
        )
      ),
      nestedField("contexts").fields(
        keywordField("publicId"),
        keywordField("path"),
        keywordField("contextType"),
        keywordField("rootId"),
        keywordField("parentIds"),
        keywordField("relevanceId"),
        booleanField("isActive"),
        booleanField("isPrimary"),
        nestedField("resourceTypes").fields(
          keywordField("id")
        )
      ),
      nestedField("embedResourcesAndIds").fields(
        keywordField("resource"),
        keywordField("id"),
        keywordField("language")
      ),
      nestedField("metaImage").fields(
        keywordField("imageId"),
        keywordField("altText"),
        keywordField("language")
      ),
      nestedField("revisionMeta").fields(
        keywordField("id"),
        dateField("revisionDate"),
        keywordField("note"),
        keywordField("status")
      ),
      keywordField("nextRevision.id"),
      keywordField("nextRevision.status"),
      textField("nextRevision.note"),
      dateField("nextRevision.revisionDate"),
      keywordField("priority"),
      keywordField("defaultParentTopicName"),
      keywordField("defaultRoot"),
      keywordField("defaultResourceTypeName")
    )
    properties(fields)
  }

  test("That building single termsAggregation works as expected") {
    val res1 = AggregationBuilder.buildTermsAggregation(Seq("draftStatus.current"), List(testMapping))
    res1 should be(
      Seq(
        termsAgg("draftStatus.current", "draftStatus.current")
          .size(50)
      )
    )
  }

  test("That building nested termsAggregation works as expected") {
    val res1 = AggregationBuilder.buildTermsAggregation(Seq("contexts.contextType"), List(testMapping))
    res1 should be(
      Seq(
        nestedAggregation("contexts", "contexts").subAggregations(
          termsAgg("contextType", "contexts.contextType").size(50)
        )
      )
    )
  }

  test("That building nested multiple layers termsAggregation works as expected") {
    val res1 = AggregationBuilder.buildTermsAggregation(Seq("contexts.resourceTypes.id"), List(testMapping))
    res1 should be(
      Seq(
        nestedAggregation("contexts", "contexts")
          .subAggregations(
            nestedAggregation("resourceTypes", "contexts.resourceTypes")
              .subAggregations(
                termsAgg("id", "contexts.resourceTypes.id").size(50)
              )
          )
      )
    )
  }

  test("That aggregating paths that requires merging works as expected") {
    val res1 = AggregationBuilder.buildTermsAggregation(
      Seq("contexts.contextType", "contexts.resourceTypes.id"),
      List(testMapping)
    )

    res1 should be(
      Seq(
        nestedAggregation("contexts", "contexts")
          .subAggregations(
            nestedAggregation("resourceTypes", "contexts.resourceTypes")
              .subAggregations(
                termsAgg("id", "contexts.resourceTypes.id").size(50)
              ),
            termsAgg("contextType", "contexts.contextType").size(50)
          )
      )
    )
  }

  test("That building multiple termsAggregation works as expected") {
    val res1 = AggregationBuilder.buildTermsAggregation(
      Seq("draftStatus.current", "draftStatus.other", "contexts.contextType", "contexts.resourceTypes.id"),
      List(testMapping)
    )
    res1 should be(
      Seq(
        termsAgg("draftStatus.current", "draftStatus.current").size(50),
        termsAgg("draftStatus.other", "draftStatus.other").size(50),
        nestedAggregation("contexts", "contexts")
          .subAggregations(
            nestedAggregation("resourceTypes", "contexts.resourceTypes")
              .subAggregations(
                termsAgg("id", "contexts.resourceTypes.id").size(50)
              ),
            termsAgg("contextType", "contexts.contextType").size(50)
          )
      )
    )
  }

  test("that passing in an empty list does not crash even though it shouldnt happen") {
    val res1 = AggregationBuilder.buildTermsAggregation(Seq.empty, List(testMapping))
    res1 should be(Seq.empty)
  }

  test("that passing in a path that doesn't exist doesn't run forever") {
    val res1 = AggregationBuilder.buildTermsAggregation(Seq("contexts.someFieldThatDoesntExist"), List(testMapping))
    res1 should be(Seq.empty)
  }
}
