/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class TaxonomyBundle(
    relevances: List[Relevance],
    resourceResourceTypeConnections: List[ResourceResourceTypeConnection],
    resourceTypes: List[ResourceType],
    resources: List[Resource],
    subjectTopicConnections: List[SubjectTopicConnection],
    subjects: List[TaxSubject],
    topicResourceConnections: List[TopicResourceConnection],
    topicSubtopicConnections: List[TopicSubtopicConnection],
    topics: List[Topic]
) {

  val topicResourceConnectionsByResourceId: Map[String, List[TopicResourceConnection]] = {
    topicResourceConnections.groupBy(_.resourceId)
  }

  val topicSubtopicConnectionsBySubTopicId: Map[String, List[TopicSubtopicConnection]] = {
    topicSubtopicConnections.groupBy(_.subtopicid)
  }

  val resourceResourceTypeConnectionsByResourceId: Map[String, List[ResourceResourceTypeConnection]] = {
    resourceResourceTypeConnections.groupBy(_.resourceId)
  }

  val subjectTopicConnectionsByTopicId: Map[String, List[SubjectTopicConnection]] =
    subjectTopicConnections.groupBy(_.topicid)

  val topicById: Map[String, Topic] = Map.from(topics.map(t => t.id -> t))
  val resourceById: Map[String, Resource] = Map.from(resources.map(r => r.id -> r))
  val subjectsById: Map[String, TaxSubject] = Map.from(subjects.map(s => s.id -> s))
  val relevancesById: Map[String, Relevance] = Map.from(relevances.map(r => r.id -> r))

  val topicsByContentUri: Map[String, List[Topic]] = {
    val contentUriToTopics = topics.flatMap(t => t.contentUri.map(cu => cu -> t))
    contentUriToTopics.groupMap(_._1)(_._2)
  }

  val resourcesByContentUri: Map[String, List[Resource]] = {
    val contentUriToResources = resources.flatMap(r => r.contentUri.map(cu => cu -> r))
    contentUriToResources.groupMap(_._1)(_._2)
  }

  /**
    * Returns a flattened list of resourceType with its subtypes
    *
    * @param resourceType A resource with subtypes
    * @return Flattened list of resourceType with subtypes.
    */
  private def getTypeAndSubtypes(resourceType: ResourceType): List[ResourceType] = {
    def getTypeAndSubtypesWithParent(resourceType: ResourceType, parents: List[ResourceType]): List[ResourceType] = {
      resourceType.subtypes match {
        case None => (parents :+ resourceType).distinct
        case Some(subtypes) =>
          subtypes.flatMap(x => getTypeAndSubtypesWithParent(x, parents :+ resourceType))
      }
    }
    getTypeAndSubtypesWithParent(resourceType, List.empty)
  }

  /**
    * Returns every parent of resourceType.
    *
    * @param resourceType ResourceType to derive parents for.
    * @return List of parents including resourceType.
    */
  def getResourceTypeParents(resourceType: ResourceType): List[ResourceType] = {
    def allTypesWithParents(allTypes: List[ResourceType],
                            parents: List[ResourceType]): List[(ResourceType, List[ResourceType])] = {
      allTypes.flatMap(resourceType => {
        val thisLevelWithParents = allTypes.map(resourceType => (resourceType, parents))

        val nextLevelWithParents = resourceType.subtypes match {
          case Some(subtypes) => allTypesWithParents(subtypes, parents :+ resourceType)
          case None           => List.empty
        }
        nextLevelWithParents ++ thisLevelWithParents
      })
    }
    allTypesWithParents(resourceTypes, List.empty).filter(x => x._1 == resourceType).flatMap(_._2).distinct
  }

  val allResourceTypes: List[ResourceType] = resourceTypes.flatMap(rt => getTypeAndSubtypes(rt))
  val allResourceTypesById: Map[String, ResourceType] = Map.from(allResourceTypes.map(rt => rt.id -> rt))

  val resourceTypeParentsByResourceTypeId: Map[String, List[ResourceType]] = {
    Map.from(allResourceTypes.map(rt => rt.id -> getResourceTypeParents(rt)))
  }

  def getResourceTopics(resource: Resource): List[Topic] = {
    val tc = topicResourceConnectionsByResourceId.getOrElse(resource.id, List.empty)
    tc.flatMap(c => topicById.get(c.topicid))
  }

  def getSubject(path: Option[String]): Option[TaxSubject] =
    path.flatMap(p => p.split('/').lift(1).flatMap(s => subjectsById.get(s"urn:$s")))

  def getObject(id: String): Option[TaxonomyElement] = {
    if (id.contains(":resource:")) {
      resourceById.get(id)
    } else if (id.contains(":topic:")) {
      topicById.get(id)
    } else if (id.contains(":subject:")) {
      subjectsById.get(id)
    } else {
      None
    }
  }
}
