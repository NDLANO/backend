/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import enumeratum._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.domain.{Responsible, Tag, Title}
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.language.Language.getSupportedLanguages
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats, Serializer}
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Concept(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[ConceptContent],
    copyright: Option[DraftCopyright],
    created: NDLADate,
    updated: NDLADate,
    updatedBy: Seq[String],
    metaImage: Seq[ConceptMetaImage],
    tags: Seq[Tag],
    subjectIds: Set[String],
    articleIds: Seq[Long],
    status: Status,
    visualElement: Seq[VisualElement],
    responsible: Option[Responsible],
    conceptType: ConceptType.Value,
    glossData: Option[GlossData],
    editorNotes: Seq[EditorNote]
) {

  lazy val supportedLanguages: Set[String] =
    getSupportedLanguages(title, content, tags, visualElement, metaImage).toSet
}
object Concept extends SQLSyntaxSupport[Concept] {
  implicit val encoder: Encoder[Concept] = deriveEncoder
  implicit val decoder: Decoder[Concept] = deriveDecoder

  override val tableName = "conceptdata"

  def fromResultSet(lp: SyntaxProvider[Concept])(rs: WrappedResultSet): Concept =
    fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Concept])(rs: WrappedResultSet): Concept = {
    implicit val formats: Formats = this.repositorySerializer ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer

    val id       = rs.long(lp.c("id"))
    val revision = rs.int(lp.c("revision"))
    val jsonStr  = rs.string(lp.c("document"))

    val meta = read[Concept](jsonStr)

    new Concept(
      id = Some(id),
      revision = Some(revision),
      meta.title,
      meta.content,
      meta.copyright,
      meta.created,
      meta.updated,
      meta.updatedBy,
      meta.metaImage,
      meta.tags,
      meta.subjectIds,
      meta.articleIds,
      meta.status,
      meta.visualElement,
      meta.responsible,
      meta.conceptType,
      meta.glossData,
      meta.editorNotes
    )
  }
  val serializers: List[Serializer[_]] = List(
    Json4s.serializer(ConceptStatus),
    new EnumNameSerializer(ConceptType),
    new EnumNameSerializer(WordClass),
    NDLADate.Json4sSerializer
  ) ++ JavaTimeSerializers.all

  val jsonEncoder: Formats = DefaultFormats ++ serializers

  val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[Concept](
      ignore("id") orElse
        ignore("revision")
    )
}

object PublishedConcept extends SQLSyntaxSupport[Concept] {
  override val tableName = "publishedconceptdata"
}

sealed trait ConceptStatus extends EnumEntry {}
object ConceptStatus extends Enum[ConceptStatus] with CirceEnum[ConceptStatus] {
  case object IN_PROGRESS       extends ConceptStatus
  case object EXTERNAL_REVIEW   extends ConceptStatus
  case object INTERNAL_REVIEW   extends ConceptStatus
  case object QUALITY_ASSURANCE extends ConceptStatus
  case object LANGUAGE          extends ConceptStatus
  case object FOR_APPROVAL      extends ConceptStatus
  case object END_CONTROL       extends ConceptStatus
  case object PUBLISHED         extends ConceptStatus
  case object UNPUBLISHED       extends ConceptStatus
  case object ARCHIVED          extends ConceptStatus

  val values: IndexedSeq[ConceptStatus] = findValues

  def valueOfOrError(s: String): Try[ConceptStatus] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          ValidationException(
            "status",
            s"'$s' is not a valid concept status. Must be one of $validStatuses"
          )
        )
    }

  def valueOf(s: String): Option[ConceptStatus] = values.find(_.toString == s.toUpperCase)

  val thatDoesNotRequireResponsible: Seq[ConceptStatus] = Seq(PUBLISHED, UNPUBLISHED, ARCHIVED)
  val thatRequiresResponsible: Set[ConceptStatus] = this.values.filterNot(thatDoesNotRequireResponsible.contains).toSet

  implicit def ordering[A <: ConceptStatus]: Ordering[ConceptStatus] =
    (x: ConceptStatus, y: ConceptStatus) => indexOf(x) - indexOf(y)
}
