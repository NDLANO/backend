package no.ndla.validation

import enumeratum.{Json4s, _}
import org.json4s.Formats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods._

object TagRules {
  case class TagAttributeRules(
      fields: Set[Field],
      mustBeDirectChildOf: Option[ParentTag],
      children: Option[ChildrenRule],
      mustContainAtLeastOneOptionalAttribute: Option[Boolean]
  ) {
    lazy val all: Set[TagAttribute] = fields.map(f => f.name)

    lazy val optional: Set[Field] = fields.filter(f => !f.validation.required)
    lazy val required: Set[Field] = fields.filter(f => f.validation.required)

    def field(tag: TagAttribute): Option[Field] = fields.find(f => f.name == tag)

    def mustContainOptionalAttribute: Boolean = this.mustContainAtLeastOneOptionalAttribute.getOrElse(false)

    def withOptionalRequired(toBeOptional: Seq[String]): TagAttributeRules = {
      val toBeOptionalEnums = toBeOptional.map(TagAttribute.withName)
      val otherFields       = fields.filterNot(f => toBeOptionalEnums.contains(f.name))
      val flipped = fields.diff(otherFields).map(f => f.copy(validation = f.validation.copy(required = false)))

      this.copy(
        fields = flipped ++ otherFields
      )
    }
  }

  case class Validation(
      dataType: AttributeType = AttributeType.STRING,
      required: Boolean = false,
      allowedHtml: Set[String] = Set.empty,
      allowedDomains: Set[String] = Set.empty,
      mustCoexistWith: List[TagAttribute] = List.empty
  )
  case class Field(name: TagAttribute, validation: Validation = Validation()) {
    override def toString: String = name.entryName
  }

  case class ParentTag(name: String, requiredAttr: List[(String, String)], conditions: Option[Condition])
  case class ChildrenRule(required: Boolean, allowedChildren: List[String])
  object ChildrenRule {
    def default: ChildrenRule = ChildrenRule(required = false, allowedChildren = List.empty)
  }
  case class Condition(childCount: String)

  object TagAttributeRules {
    def empty: TagAttributeRules = TagAttributeRules(Set.empty, None, None, None)
  }

  def convertJsonStrToAttributeRules(jsonStr: String): Map[String, TagAttributeRules] = {
    implicit val formats: Formats =
      org.json4s.DefaultFormats + Json4s.serializer(TagAttribute) + Json4s.serializer(AttributeType)

    (parse(jsonStr) \ "attributes")
      .extract[JObject]
      .obj
      .map { case (fieldName, fieldValue) =>
        fieldName -> fieldValue.extract[TagAttributeRules]
      }
      .toMap
  }
}

sealed abstract class AttributeType extends EnumEntry
object AttributeType extends Enum[AttributeType] {
  val values: IndexedSeq[AttributeType] = findValues
  case object BOOLEAN extends AttributeType
  case object EMAIL   extends AttributeType
  case object NUMBER  extends AttributeType
  case object STRING  extends AttributeType
  case object URL     extends AttributeType
}

sealed abstract class TagAttribute(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}
object TagAttribute extends Enum[TagAttribute] {
  val values: IndexedSeq[TagAttribute] = findValues

  case object Align          extends TagAttribute("align")
  case object Class          extends TagAttribute("class")
  case object Colspan        extends TagAttribute("colspan")
  case object Dir            extends TagAttribute("dir")
  case object Headers        extends TagAttribute("headers")
  case object Href           extends TagAttribute("href")
  case object Id             extends TagAttribute("id")
  case object Lang           extends TagAttribute("lang")
  case object Name           extends TagAttribute("name")
  case object Rel            extends TagAttribute("rel")
  case object Rowspan        extends TagAttribute("rowspan")
  case object Scope          extends TagAttribute("scope")
  case object Span           extends TagAttribute("span")
  case object Start          extends TagAttribute("start")
  case object Style          extends TagAttribute("style")
  case object Target         extends TagAttribute("target")
  case object Title          extends TagAttribute("title")
  case object Valign         extends TagAttribute("valign")
  case object XMLNsAttribute extends TagAttribute("xmlns")

  case object DataAccount             extends TagAttribute("data-account")
  case object DataAlign               extends TagAttribute("data-align")
  case object DataAlt                 extends TagAttribute("data-alt")
  case object DataArticleId           extends TagAttribute("data-article-id")
  case object DataAuthor              extends TagAttribute("data-author")
  case object DataAuthors             extends TagAttribute("data-authors")
  case object DataBackground          extends TagAttribute("data-background")
  case object DataBlob                extends TagAttribute("data-blob")
  case object DataBlobColor           extends TagAttribute("data-blob-color")
  case object DataBorder              extends TagAttribute("data-border")
  case object DataCaption             extends TagAttribute("data-caption")
  case object DataColumns             extends TagAttribute("data-columns")
  case object DataContent             extends TagAttribute("data-code-content")
  case object DataContentId           extends TagAttribute("data-content-id")
  case object DataContentType         extends TagAttribute("data-content-type")
  case object DataDate                extends TagAttribute("data-date")
  case object DataDescription         extends TagAttribute("data-description")
  case object DataDescriptionLanguage extends TagAttribute("data-description-language")
  case object DataDisplay             extends TagAttribute("data-display")
  case object DataEdition             extends TagAttribute("data-edition")
  case object DataEmail               extends TagAttribute("data-email")
  case object DataFocalX              extends TagAttribute("data-focal-x")
  case object DataFocalY              extends TagAttribute("data-focal-y")
  case object DataFormat              extends TagAttribute("data-code-format")
  case object DataHeadingLevel        extends TagAttribute("data-heading-level")
  case object DataHeight              extends TagAttribute("data-height")
  case object DataImageId             extends TagAttribute("data-imageid")
  case object DataImageSide           extends TagAttribute("data-image-side")
  case object DataImage_Id            extends TagAttribute("data-image-id")
  case object DataIsDecorative        extends TagAttribute("data-is-decorative")
  case object DataJobTitle            extends TagAttribute("data-job-title")
  case object DataLanguage            extends TagAttribute("data-language")
  case object DataLinkText            extends TagAttribute("data-link-text")
  case object DataLowerRightX         extends TagAttribute("data-lower-right-x")
  case object DataLowerRightY         extends TagAttribute("data-lower-right-y")
  case object DataMessage             extends TagAttribute("data-message")
  case object DataNRKVideoId          extends TagAttribute("data-nrk-video-id")
  case object DataName                extends TagAttribute("data-name")
  case object DataOpenIn              extends TagAttribute("data-open-in")
  case object DataParallaxCell        extends TagAttribute("data-parallax-cell")
  case object DataPath                extends TagAttribute("data-path")
  case object DataPlayer              extends TagAttribute("data-player")
  case object DataPublisher           extends TagAttribute("data-publisher")
  case object DataRecursive           extends TagAttribute("data-recursive")
  case object DataResource            extends TagAttribute("data-resource")
  case object DataResource_Id         extends TagAttribute("data-resource_id")
  case object DataSize                extends TagAttribute("data-size")
  case object DataSubjectId           extends TagAttribute("data-subject-id")
  case object DataSubtitle            extends TagAttribute("data-subtitle")
  case object DataTag                 extends TagAttribute("data-tag")
  case object DataTitle               extends TagAttribute("data-title")
  case object DataTitleLanguage       extends TagAttribute("data-title-language")
  case object DataType                extends TagAttribute("data-type")
  case object DataUpperLeftX          extends TagAttribute("data-upper-left-x")
  case object DataUpperLeftY          extends TagAttribute("data-upper-left-y")
  case object DataUrl                 extends TagAttribute("data-url")
  case object DataUrlText             extends TagAttribute("data-url-text")
  case object DataVideoId             extends TagAttribute("data-videoid")
  case object DataWidth               extends TagAttribute("data-width")
  case object DataYear                extends TagAttribute("data-year")
}
