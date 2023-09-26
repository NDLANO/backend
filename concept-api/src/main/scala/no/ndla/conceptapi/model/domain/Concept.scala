/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.common.model.domain.{Responsible, Tag, Title}
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.conceptapi.Props
import no.ndla.language.Language.getSupportedLanguages
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
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
    glossData: Option[GlossData]
) {

  lazy val supportedLanguages: Set[String] =
    getSupportedLanguages(title, content, tags, visualElement, metaImage).toSet
}
trait DBConcept {
  this: Props =>

  object Concept extends SQLSyntaxSupport[Concept] {
    override val tableName                  = "conceptdata"
    override val schemaName: Option[String] = Some(props.MetaSchema)

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
        meta.glossData
      )
    }

    val jsonEncoder: Formats = DefaultFormats +
      new EnumNameSerializer(ConceptStatus) +
      new EnumNameSerializer(ConceptType) +
      new EnumNameSerializer(WordClass) ++
      JavaTimeSerializers.all +
      NDLADate.Json4sSerializer

    val repositorySerializer: Formats = jsonEncoder +
      FieldSerializer[Concept](
        ignore("id") orElse
          ignore("revision")
      )
  }

  object PublishedConcept extends SQLSyntaxSupport[Concept] {
    override val tableName                  = "publishedconceptdata"
    override val schemaName: Option[String] = Some(props.MetaSchema)
  }
}

object ConceptStatus extends Enumeration {
  val IN_PROGRESS, EXTERNAL_REVIEW, INTERNAL_REVIEW, QUALITY_ASSURANCE, LANGUAGE, FOR_APPROVAL, END_CONTROL, PUBLISHED,
      UNPUBLISHED, ARCHIVED = Value

  def valueOfOrError(s: String): Try[ConceptStatus.Value] =
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

  def valueOf(s: String): Option[ConceptStatus.Value] = values.find(_.toString == s.toUpperCase)

  val thatDoesNotRequireResponsible: Seq[ConceptStatus.Value] = Seq(PUBLISHED, UNPUBLISHED, ARCHIVED)
  val thatRequiresResponsible: ConceptStatus.ValueSet = this.values.filterNot(thatDoesNotRequireResponsible.contains)
}
