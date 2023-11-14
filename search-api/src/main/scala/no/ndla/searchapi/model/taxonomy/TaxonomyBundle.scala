/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization

import java.io.FileInputStream
import scala.util.{Failure, Success, Try}

sealed trait NodeStorage {
  def getOrElse(contentUri: String, default: => List[Node]): List[Node]
  def contains(contentUri: String): Boolean
}

case class TaxonomyBundle(nodeByContentUri: NodeStorage) extends AutoCloseable {
  override def close(): Unit = nodeByContentUri match {
    case tmpNodes: TmpNodes => tmpNodes.close()
    case _                  =>
  }
}

case class TmpNodes(tmpDir: String, isPublished: Boolean) extends NodeStorage with AutoCloseable with StrictLogging {
  override def contains(contentUri: String): Boolean = getOrElse(contentUri, Nil).nonEmpty
  override def getOrElse(contentUri: String, default: => List[Node]): List[Node] = {
    val pub  = if (isPublished) "published" else "draft"
    val path = s"$tmpDir/${contentUri}_$pub.json"
    val f    = new java.io.File(path)
    if (f.exists()) {
      val stream = new FileInputStream(f)
      val strs   = new String(stream.readAllBytes()).split("\n").toList
      val nodes = strs.traverse(str => {
        Try(Serialization.read[Node](str)(TaxonomyBundle.formats, implicitly[Manifest[Node]]))
      })
      nodes match {
        case Success(value) => return value
        case Failure(ex)    => logger.error(s"Failed to deserialize taxonomy node: '$path'", ex)
      }
    }
    default
  }

  override def close(): Unit = {
    logger.info(s"Cleaning up TaxonomyBundle tmp files in '$tmpDir'")
    val dir = new java.io.File(tmpDir)
    if (dir.exists()) {
      dir.listFiles().foreach(_.delete())
      dir.delete(): Unit
    }
  }
}

case class InMemoryNodeStorage(nodes: List[Node]) extends NodeStorage {
  private val nodeMap = {
    nodes
      .flatMap(t => t.contentUri.map(cu => cu -> t))
      .groupMap(_._1)(_._2)
  }

  override def contains(contentUri: String): Boolean = nodeMap.contains(contentUri)
  override def getOrElse(contentUri: String, default: => List[Node]): List[Node] = {
    nodeMap.getOrElse(contentUri, default)
  }
}

object TaxonomyBundle {
  implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis + Json4s.serializer(NodeType)
  def apply(tmpDir: String, isPublished: Boolean): TaxonomyBundle = {
    val storage = TmpNodes(tmpDir, isPublished)
    new TaxonomyBundle(storage)
  }

  def apply(nodes: List[Node]): TaxonomyBundle = {
    val storage = InMemoryNodeStorage(nodes)
    new TaxonomyBundle(storage)
  }
}
