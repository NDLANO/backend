package no.ndla.validation

import org.json4s.Formats
import org.json4s.JsonAST.JObject
import org.json4s.ext._
import org.json4s.native.JsonMethods._

object TagRules {
  case class TagAttributeRules(
      required: Set[TagAttributes.Value],
      requiredNonEmpty: Set[TagAttributes.Value],
      optional: Seq[Set[TagAttributes.Value]],
      validSrcDomains: Option[Seq[String]],
      mustBeDirectChildOf: Option[ParentTag],
      children: Option[ChildrenRule],
      mustContainAtLeastOneOptionalAttribute: Option[Boolean]
  ) {
    lazy val all: Set[TagAttributes.Value] = required ++ requiredNonEmpty ++ optional.flatten

    def mustContainOptionalAttribute: Boolean = this.mustContainAtLeastOneOptionalAttribute.getOrElse(false)

    def withOptionalRequired(toBeOptional: Seq[String]): TagAttributeRules = {
      val toBeOptionalEnums = toBeOptional.flatMap(TagAttributes.valueOf)
      val newReq            = required.filterNot(toBeOptionalEnums.contains)
      val newOpt            = optional ++ toBeOptionalEnums.map(o => Set(o))

      this.copy(
        required = newReq,
        optional = newOpt
      )
    }
  }

  case class ParentTag(name: String, requiredAttr: List[(String, String)], conditions: Option[Condition])
  case class ChildrenRule(required: Boolean, allowedChildren: List[String])
  object ChildrenRule {
    def default: ChildrenRule = ChildrenRule(required = false, allowedChildren = List.empty)
  }
  case class Condition(childCount: String)

  object TagAttributeRules {
    def empty: TagAttributeRules = TagAttributeRules(Set.empty, Set.empty, Seq.empty, None, None, None, None)
  }

  def convertJsonStrToAttributeRules(jsonStr: String): Map[String, TagAttributeRules] = {
    implicit val formats: Formats = org.json4s.DefaultFormats + new EnumNameSerializer(TagAttributes)

    (parse(jsonStr) \ "attributes")
      .extract[JObject]
      .obj
      .map { case (fieldName, fieldValue) =>
        fieldName -> fieldValue.extract[TagAttributeRules]
      }
      .toMap
  }
}

object TagAttributes extends Enumeration {
  val DataUrl: TagAttributes.Value                 = Value("data-url")
  val DataAlt: TagAttributes.Value                 = Value("data-alt")
  val DataSize: TagAttributes.Value                = Value("data-size")
  val DataAlign: TagAttributes.Value               = Value("data-align")
  val DataWidth: TagAttributes.Value               = Value("data-width")
  val DataHeight: TagAttributes.Value              = Value("data-height")
  val DataPlayer: TagAttributes.Value              = Value("data-player")
  val DataMessage: TagAttributes.Value             = Value("data-message")
  val DataCaption: TagAttributes.Value             = Value("data-caption")
  val DataAccount: TagAttributes.Value             = Value("data-account")
  val DataVideoId: TagAttributes.Value             = Value("data-videoid")
  val DataImageId: TagAttributes.Value             = Value("data-imageid")
  val DataImage_Id: TagAttributes.Value            = Value("data-image-id")
  val DataResource: TagAttributes.Value            = Value("data-resource")
  val DataLanguage: TagAttributes.Value            = Value("data-language")
  val DataAuthor: TagAttributes.Value              = Value("data-author")
  val DataLinkText: TagAttributes.Value            = Value("data-link-text")
  val DataOpenIn: TagAttributes.Value              = Value("data-open-in")
  val DataContentId: TagAttributes.Value           = Value("data-content-id")
  val DataContentType: TagAttributes.Value         = Value("data-content-type")
  val DataNRKVideoId: TagAttributes.Value          = Value("data-nrk-video-id")
  val DataResource_Id: TagAttributes.Value         = Value("data-resource_id")
  val DataTitle: TagAttributes.Value               = Value("data-title")
  val DataType: TagAttributes.Value                = Value("data-type")
  val DataYear: TagAttributes.Value                = Value("data-year")
  val DataEdition: TagAttributes.Value             = Value("data-edition")
  val DataPublisher: TagAttributes.Value           = Value("data-publisher")
  val DataAuthors: TagAttributes.Value             = Value("data-authors")
  val DataArticleId: TagAttributes.Value           = Value("data-article-id")
  val DataPath: TagAttributes.Value                = Value("data-path")
  val DataFormat: TagAttributes.Value              = Value("data-code-format")
  val DataContent: TagAttributes.Value             = Value("data-code-content")
  val DataDisplay: TagAttributes.Value             = Value("data-display")
  val DataRecursive: TagAttributes.Value           = Value("data-recursive")
  val DataSubjectId: TagAttributes.Value           = Value("data-subject-id")
  val DataTag: TagAttributes.Value                 = Value("data-tag")
  val DataEmail: TagAttributes.Value               = Value("data-email")
  val DataBlob: TagAttributes.Value                = Value("data-blob")
  val DataBlobColor: TagAttributes.Value           = Value("data-blob-color")
  val DataName: TagAttributes.Value                = Value("data-name")
  val DataDescription: TagAttributes.Value         = Value("data-description")
  val DataJobTitle: TagAttributes.Value            = Value("data-job-title")
  val DataUpperLeftY: TagAttributes.Value          = Value("data-upper-left-y")
  val DataUpperLeftX: TagAttributes.Value          = Value("data-upper-left-x")
  val DataLowerRightY: TagAttributes.Value         = Value("data-lower-right-y")
  val DataLowerRightX: TagAttributes.Value         = Value("data-lower-right-x")
  val DataFocalX: TagAttributes.Value              = Value("data-focal-x")
  val DataFocalY: TagAttributes.Value              = Value("data-focal-y")
  val DataIsDecor: TagAttributes.Value             = Value("data-isdecor")
  val DataSubtitle: TagAttributes.Value            = Value("data-subtitle")
  val DataColumns: TagAttributes.Value             = Value("data-columns")
  val DataBorder: TagAttributes.Value              = Value("data-border")
  val DataBackground: TagAttributes.Value          = Value("data-background")
  val DataTitleLanguage: TagAttributes.Value       = Value("data-title-language")
  val DataDescriptionLanguage: TagAttributes.Value = Value("data-description-language")
  val DataHeadingLevel: TagAttributes.Value        = Value("data-heading-level")
  val DataUrlText: TagAttributes.Value             = Value("data-url-text")
  val DataImageBeforeId: TagAttributes.Value       = Value("data-image-before-id")
  val DataImageAfterid: TagAttributes.Value        = Value("data-image-after-id")
  val XMLNsAttribute: TagAttributes.Value          = Value("xmlns")

  val Href: TagAttributes.Value    = Value("href")
  val Title: TagAttributes.Value   = Value("title")
  val Align: TagAttributes.Value   = Value("align")
  val Valign: TagAttributes.Value  = Value("valign")
  val Target: TagAttributes.Value  = Value("target")
  val Rel: TagAttributes.Value     = Value("rel")
  val Class: TagAttributes.Value   = Value("class")
  val Lang: TagAttributes.Value    = Value("lang")
  val Rowspan: TagAttributes.Value = Value("rowspan")
  val Colspan: TagAttributes.Value = Value("colspan")
  val Name: TagAttributes.Value    = Value("name")
  val Start: TagAttributes.Value   = Value("start")
  val Style: TagAttributes.Value   = Value("style")
  val Span: TagAttributes.Value    = Value("span")
  val Id: TagAttributes.Value      = Value("id")
  val Scope: TagAttributes.Value   = Value("scope")
  val Headers: TagAttributes.Value = Value("headers")

  private[validation] def getOrCreate(s: String): TagAttributes.Value = {
    valueOf(s).getOrElse(Value(s))
  }

  def all: Set[String] = TagAttributes.values.map(_.toString)

  def valueOf(s: String): Option[TagAttributes.Value] = {
    TagAttributes.values.find(_.toString == s)
  }
}
