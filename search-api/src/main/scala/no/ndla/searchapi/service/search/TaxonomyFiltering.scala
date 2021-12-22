/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, NestedQuery, Query}
import no.ndla.searchapi.model.domain.article.LearningResourceType

trait TaxonomyFiltering {

  protected def relevanceFilter(relevanceIds: List[String], subjectIds: List[String]): Option[BoolQuery] =
    if (relevanceIds.isEmpty) None
    else
      Some(
        boolQuery().should(
          relevanceIds.map(
            relevanceId =>
              nestedQuery("contexts").query(
                boolQuery().must(
                  termQuery("contexts.relevanceId", relevanceId),
                  boolQuery().should(subjectIds.map(sId => termQuery("contexts.subjectId", sId)))
                )
            )
          )
        ))

  protected def subjectFilter(subjects: List[String]): Option[NestedQuery] =
    if (subjects.isEmpty) None
    else
      Some(
        nestedQuery("contexts")
          .query(
            boolQuery().should(
              subjects.map(
                subjectId => termQuery(s"contexts.subjectId", subjectId)
              ))
          ))

  protected def topicFilter(topics: List[String]): Option[NestedQuery] =
    if (topics.isEmpty) None
    else
      Some(
        nestedQuery("contexts")
          .query(
            boolQuery().should(
              topics.map(
                topicId => termQuery("contexts.parentTopicIds", topicId)
              ))
          ))

  protected def resourceTypeFilter(resourceTypes: List[String], filterByNoResourceType: Boolean): Option[Query] = {
    if (resourceTypes.isEmpty) {
      if (filterByNoResourceType) {
        Some(
          boolQuery().not(
            nestedQuery("contexts.resourceTypes").query(existsQuery("contexts.resourceTypes"))
          )
        )
      } else { None }
    } else {
      Some(
        nestedQuery("contexts.resourceTypes")
          .query(
            boolQuery().should(
              resourceTypes.map(
                resourceTypeId => termQuery("contexts.resourceTypes.id", resourceTypeId)
              ))
          ))
    }
  }

  protected def contextTypeFilter(contextTypes: List[LearningResourceType.Value]): Option[BoolQuery] =
    if (contextTypes.isEmpty) None
    else {
      val articleTypeQuery = contextTypes.map(ct => termQuery("articleType", ct.toString))

      val notArticleTypeQuery = if (contextTypes.contains(LearningResourceType.LearningPath)) {
        List(boolQuery().not(existsQuery("articleType")))
      } else {
        List.empty
      }

      val taxonomyContextQuery = contextTypes.map(
        ct =>
          nestedQuery("contexts").query(
            termQuery("contexts.contextType", ct.toString)
        ))

      Some(
        boolQuery().should(articleTypeQuery ++ taxonomyContextQuery ++ notArticleTypeQuery)
      )
    }

}
