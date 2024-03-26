/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import no.ndla.searchapi.model.domain.LearningResourceType

trait TaxonomyFiltering {

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

  protected def subjectFilter(subjects: List[String], filterInactive: Boolean): Option[NestedQuery] =
    if (subjects.isEmpty) None
    else {
      val subjectQueries = subjects.map(subjectId =>
        if (filterInactive)
          boolQuery().must(booleanMust("contexts.rootId", subjectId), booleanMust("contexts.isActive", "true"))
        else booleanMust("contexts.rootId", subjectId)
      )
      Some(nestedQuery("contexts", boolQuery().should(subjectQueries)))
    }

  protected def topicFilter(topics: List[String], filterInactive: Boolean): Option[NestedQuery] =
    if (topics.isEmpty) None
    else {
      val subjectQueries = topics.map(subjectId =>
        if (filterInactive)
          boolQuery().must(booleanMust("contexts.parentIds", subjectId), booleanMust("contexts.isActive", "true"))
        else booleanMust("contexts.parentIds", subjectId)
      )
      Some(nestedQuery("contexts", boolQuery().should(subjectQueries)))
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

  protected def contextTypeFilter(contextTypes: List[LearningResourceType.Value]): Option[BoolQuery] =
    if (contextTypes.isEmpty) None
    else {
      val taxonomyContextQuery =
        contextTypes.map(ct => nestedQuery("contexts", termQuery("contexts.contextType", ct.toString)))

      Some(boolQuery().should(taxonomyContextQuery))
    }

  protected def contextActiveFilter(filterInactive: Boolean): Option[Query] =
    if (filterInactive) {
      val contextActiveQuery = nestedQuery("contexts", termQuery("contexts.isActive", true))
      Some(contextActiveQuery)
    } else None
}
