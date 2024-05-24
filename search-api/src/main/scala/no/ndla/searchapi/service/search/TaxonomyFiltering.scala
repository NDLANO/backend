/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import no.ndla.searchapi.model.domain.LearningResourceType

trait TaxonomyFiltering {

  private val notConceptType = LearningResourceType.values
    .filter(x => x != LearningResourceType.Concept && x != LearningResourceType.Gloss)
    .map(_.entryName)

  private val mustBeConceptQuery = termsQuery(
    "learningResourceType",
    Seq(LearningResourceType.Concept.entryName, LearningResourceType.Gloss.entryName)
  )

  private val mustNotBeConceptQuery = termsQuery("learningResourceType", notConceptType)

  private def mustBeConceptOr(query: Query): Query = {
    val newQuery = query match {
      case nested: NestedQuery if nested.path == "contexts" => nested.ignoreUnmapped(true)
      case query                                            => query
    }
    boolQuery().should(newQuery, mustBeConceptQuery)
  }
  def mustBeNotConceptOr(query: Query): Query = {
    val newQuery = query match {
      case nested: NestedQuery if nested.path == "contexts" => nested.ignoreUnmapped(true)
      case query                                            => query
    }
    boolQuery().should(newQuery, mustNotBeConceptQuery)
  }

  protected def relevanceFilter(relevanceIds: List[String], subjectIds: List[String]): Option[BoolQuery] =
    if (relevanceIds.isEmpty) None
    else
      Some(
        boolQuery().should(
          relevanceIds.map(relevanceId =>
            nestedQuery(
              "contexts",
              boolQuery().must(
                termQuery("contexts.relevanceId", relevanceId),
                boolQuery().should(subjectIds.map(sId => termQuery("contexts.rootId", sId)))
              )
            )
          )
        )
      )

  private val booleanMust: (String, String) => BoolQuery = (field: String, id: String) =>
    boolQuery().must(termQuery(field, id))

  protected def subjectFilter(subjects: List[String], filterInactive: Boolean): Option[Query] =
    if (subjects.isEmpty) None
    else {
      val subjectQueries = subjects.map(subjectId =>
        if (filterInactive)
          boolQuery().must(
            booleanMust("contexts.rootId", subjectId),
            booleanMust("contexts.isActive", "true")
          )
        else booleanMust("contexts.rootId", subjectId)
      )
      Some(
        mustBeConceptOr(
          nestedQuery("contexts", boolQuery().should(subjectQueries)).ignoreUnmapped(true)
        )
      )
    }

  protected def topicFilter(topics: List[String], filterInactive: Boolean): Option[Query] =
    if (topics.isEmpty) None
    else {
      val subjectQueries = topics.map(subjectId =>
        if (filterInactive)
          boolQuery().must(
            booleanMust("contexts.parentIds", subjectId),
            booleanMust("contexts.isActive", "true")
          )
        else booleanMust("contexts.parentIds", subjectId)
      )
      Some(mustBeConceptOr(nestedQuery("contexts", boolQuery().should(subjectQueries)).ignoreUnmapped(true)))
    }

  protected def resourceTypeFilter(resourceTypes: List[String], filterByNoResourceType: Boolean): Option[Query] = {
    if (resourceTypes.isEmpty) {
      if (filterByNoResourceType) {
        Some(
          boolQuery().not(
            nestedQuery("contexts.resourceTypes", existsQuery("contexts.resourceTypes"))
          )
        )
      } else { None }
    } else {
      Some(
        nestedQuery(
          "contexts.resourceTypes",
          boolQuery().should(
            resourceTypes.map(resourceTypeId => termQuery("contexts.resourceTypes.id", resourceTypeId))
          )
        )
      )
    }
  }

  protected def contextActiveFilter(filterInactive: Boolean): Option[Query] =
    if (filterInactive) {
      val contextActiveQuery = nestedQuery("contexts", termQuery("contexts.isActive", true)).ignoreUnmapped(true)
      Some(mustBeConceptOr(contextActiveQuery))
    } else None
}
