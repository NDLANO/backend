/*
 * Part of NDLA search-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.searchapi.model.api

import cats.implicits._
import no.ndla.searchapi.model.taxonomy._

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** Helper class to work with a path of taxonomy topics */
case class TopicPath(
    base: Topic,
    connection: Option[TopicSubtopicConnection],
    child: Option[TopicPath]
) {
  def idPath: List[String] = this.path.map(_.id)

  def smallestChild: TopicPath = {
    @tailrec
    def recursiveChild(tp: TopicPath): TopicPath = {
      tp.child match {
        case Some(child) => recursiveChild(child)
        case None        => tp
      }
    }

    recursiveChild(this)
  }

  def allVisible(): Boolean = {
    @tailrec
    def allVisibleRecursive(rest: TopicPath): Boolean = {
      val isVisible = rest.base.metadata.forall(_.visible)
      if (!isVisible) false
      else {
        rest.child match {
          case Some(child) => allVisibleRecursive(child)
          case None        => isVisible
        }
      }
    }

    allVisibleRecursive(this)
  }

  def path: List[Topic] = {
    @tailrec
    def buildPath(rest: TopicPath, p: List[Topic]): List[Topic] = {
      rest.child match {
        case None     => p
        case Some(ch) => buildPath(ch, p :+ ch.base)
      }
    }

    buildPath(this, List(base))
  }

  def build(bundle: TaxonomyBundle): Try[TopicPath] = {
    @tailrec
    def recursiveBuild(topicPath: TopicPath): Try[TopicPath] = {
      val parentConnections = bundle.topicSubtopicConnectionsBySubTopicId.getOrElse(topicPath.base.id, List.empty)
      val parents = parentConnections.flatMap(pc => {
        bundle.topicById.get(pc.topicid).map(t => t -> pc)
      })

      parents match {
        case head :: Nil =>
          val next = TopicPath(head._1, head._2.some, topicPath.some)
          recursiveBuild(next)
        case Nil =>
          Success(topicPath)
        case _ =>
          Failure(
            TaxonomyException("Got path with more than one topic parent, this doesn't make sense, failing...")
          )
      }
    }

    recursiveBuild(this)
  }
}

object TopicPath {
  def from(topic: Topic, bundle: TaxonomyBundle): Try[TopicPath] =
    new TopicPath(base = topic, connection = None, child = None).build(bundle)
}
