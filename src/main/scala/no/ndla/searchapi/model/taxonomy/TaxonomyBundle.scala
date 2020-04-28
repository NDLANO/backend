/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class TaxonomyBundle(
    filters: List[Filter],
    relevances: List[Relevance],
    resourceFilterConnections: List[ResourceFilterConnection],
    resourceResourceTypeConnections: List[ResourceResourceTypeConnection],
    resourceTypes: List[ResourceType],
    resources: List[Resource],
    subjectTopicConnections: List[SubjectTopicConnection],
    subjects: List[TaxSubject],
    topicFilterConnections: List[TopicFilterConnection],
    topicResourceConnections: List[TopicResourceConnection],
    topicSubtopicConnections: List[TopicSubtopicConnection],
    topicResourceTypeConnections: List[TopicResourceTypeConnection],
    topics: List[Topic]
) {

  def getResourceTopics(resource: Resource): List[Topic] = {
    val tc = topicResourceConnections.filter(_.resourceId == resource.id)
    topics.filter(topic => tc.map(_.topicid).contains(topic.id))
  }

  def getSubject(path: Option[String]): Option[TaxSubject] =
    path.flatMap(p => p.split('/').lift(1).flatMap(s => subjects.find(_.id == s"urn:$s")))

  def getObject(id: String): Option[TaxonomyElement] = {
    if (id.contains(":resource:")) {
      resources.find(_.id == id)
    } else if (id.contains(":topic:")) {
      topics.find(_.id == id)
    } else if (id.contains(":subject:")) {
      subjects.find(_.id == id)
    } else {
      None
    }
  }
}
