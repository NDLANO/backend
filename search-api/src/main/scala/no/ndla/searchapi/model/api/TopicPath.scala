/*
 * Part of NDLA search-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.searchapi.model.api

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.searchapi.model.taxonomy._

import scala.annotation.tailrec
import scala.util.{Success, Try}

object TopicPath extends StrictLogging {
  def from(topic: Topic, bundle: TaxonomyBundle): Try[TopicPath] =
    TopicPath(base = topic, connection = None, child = None).build(bundle)

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
        val parentConnections = bundle.getTopicParentConnections(topicPath.base.id)
        val parents = parentConnections.flatMap(pc => {
          bundle.topicById.get(pc.topicid).map(t => t -> pc)
        })

        parents match {
          case Nil =>
            Success(topicPath)
          case head :: tail =>
            if (tail.nonEmpty) {
              // FIXME: Something is weird if we have more than one parent topic
              //        This seems to happen sometimes either way. Not sure why, lets log it and move on for now.
              val all = head :: tail
              val linesToLog = all.zipWithIndex.map { case ((topic, topicConnection), idx) =>
                s"Parent topic $idx -> topicId: ${topic.id}, connection: (connectionId: ${topicConnection.id}, topicId: ${topicConnection.topicid}, subtopicId: ${topicConnection.subtopicid})"
              }
              logger.error(s"Got ${all.size} parentTopics. This is weird, we got:\n${linesToLog.mkString("\n")}")
            }

            val next = TopicPath(head._1, head._2.some, topicPath.some)
            recursiveBuild(next)
        }
      }

      recursiveBuild(this)
    }
  }
}
