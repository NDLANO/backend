/*
 * Part of NDLA validation
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.validation

import enumeratum.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, parser}
import no.ndla.common.CirceUtil.getOrDefault
import no.ndla.common.model.TagAttribute

object TagRules {
  case class TagAttributeRules(
      fields: Set[Field],
      mustBeDirectChildOf: Option[ParentTag],
      children: Option[ChildrenRule],
      mustContainAtLeastOneOptionalAttribute: Option[Boolean],
  ) {
    lazy val all: Set[TagAttribute] = fields.map(f => f.name)

    lazy val optional: Set[Field] = fields.filter(f => !f.validation.required)
    lazy val required: Set[Field] = fields.filter(f => f.validation.required)

    def field(tag: TagAttribute): Option[Field] = fields.find(f => f.name == tag)

    def mustContainOptionalAttribute: Boolean = this.mustContainAtLeastOneOptionalAttribute.getOrElse(false)

    def withOptionalRequired(toBeOptional: Seq[String]): TagAttributeRules = {
      val toBeOptionalEnums = toBeOptional.map(TagAttribute.withName)
      val otherFields       = fields.filterNot(f => toBeOptionalEnums.contains(f.name))
      val flipped           = fields.diff(otherFields).map(f => f.copy(validation = f.validation.copy(required = false)))

      this.copy(fields = flipped ++ otherFields)
    }
  }

  case class Validation(
      dataType: AttributeType = AttributeType.STRING,
      required: Boolean = false,
      allowedValues: Set[String] = Set.empty,
      allowedHtml: Set[String] = Set.empty,
      allowedDomains: Set[String] = Set.empty,
      mustCoexistWith: List[TagAttribute] = List.empty,
  )

  private object Validation {
    implicit val encoder: Encoder[Validation] = deriveEncoder
    implicit val decoder: Decoder[Validation] = Decoder.instance { cur =>
      for {
        dataType        <- getOrDefault[AttributeType](cur, "dataType", AttributeType.STRING)
        required        <- getOrDefault(cur, "required", false)
        allowedValues   <- getOrDefault(cur, "allowedValues", Set.empty[String])
        allowedHtml     <- getOrDefault(cur, "allowedHtml", Set.empty[String])
        allowedDomains  <- getOrDefault(cur, "allowedDomains", Set.empty[String])
        mustCoexistWith <- getOrDefault(cur, "mustCoexistWith", List.empty[TagAttribute])
      } yield Validation(
        dataType = dataType,
        required = required,
        allowedValues = allowedValues,
        allowedHtml = allowedHtml,
        allowedDomains = allowedDomains,
        mustCoexistWith = mustCoexistWith,
      )
    }

    def default: Validation = Validation()
  }
  case class Field(name: TagAttribute, validation: Validation) {
    override def toString: String = name.entryName
  }
  object Field {
    implicit val encoder: Encoder[Field] = deriveEncoder
    implicit val decoder: Decoder[Field] = Decoder.instance(cur => {
      for {
        name       <- cur.downField("name").as[TagAttribute]
        validation <- cur.downField("validation").as[Option[Validation]].map(_.getOrElse(Validation.default))
      } yield Field(name, validation)
    })
  }

  case class ParentTag(name: String, requiredAttr: List[(String, String)], conditions: Option[Condition])
  object ParentTag {
    implicit val encoder: Encoder[ParentTag] = deriveEncoder
    implicit val decoder: Decoder[ParentTag] = deriveDecoder
  }
  case class ChildrenRule(required: Boolean, allowedChildren: Option[List[String]])
  object ChildrenRule {
    def default: ChildrenRule = ChildrenRule(required = false, allowedChildren = None)

    implicit val encoder: Encoder[ChildrenRule] = deriveEncoder
    implicit val decoder: Decoder[ChildrenRule] = deriveDecoder
  }
  case class Condition(childCount: String)
  object Condition {
    implicit val encoder: Encoder[Condition] = deriveEncoder
    implicit val decoder: Decoder[Condition] = deriveDecoder
  }

  object TagAttributeRules {
    def empty: TagAttributeRules = TagAttributeRules(Set.empty, None, None, None)

    implicit val encoder: Encoder[TagAttributeRules] = deriveEncoder
    implicit val decoder: Decoder[TagAttributeRules] = deriveDecoder
  }

  def convertJsonStrToAttributeRules(jsonStr: String): Map[String, TagAttributeRules] = {
    val parsed = parser.parse(jsonStr).toTry
    val result = parsed.get.hcursor.downField("attributes").as[Map[String, TagAttributeRules]].toTry
    result.get
  }
}

sealed abstract class AttributeType extends EnumEntry
object AttributeType                extends Enum[AttributeType] with CirceEnum[AttributeType] {
  val values: IndexedSeq[AttributeType] = findValues
  case object BOOLEAN extends AttributeType
  case object EMAIL   extends AttributeType
  case object ENUM    extends AttributeType
  case object JSON    extends AttributeType
  case object LIST    extends AttributeType
  case object NUMBER  extends AttributeType
  case object STRING  extends AttributeType
  case object TEXT    extends AttributeType
  case object URL     extends AttributeType
}
