package no.ndla.validation

import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods._
import org.json4s.ext._

object TagRules {
  case class TagAttributeRules(required: Set[TagAttributes.Value],
                               optional: Seq[Set[TagAttributes.Value]],
                               validSrcDomains: Option[Seq[String]],
                               mustBeDirectChildOf: Option[ParentTag],
                               mustContainAtLeastOneOptionalAttribute: Boolean = false) {
    lazy val all: Set[TagAttributes.Value] = required ++ optional.flatten

    def withOptionalRequired(toBeOptional: Seq[String]) = {
      val toBeOptionalEnums = toBeOptional.flatMap(TagAttributes.valueOf)
      val newReq = required.filterNot(toBeOptionalEnums.contains)
      val newOpt = optional ++ toBeOptionalEnums.map(o => Set(o))

      this.copy(
        required = newReq,
        optional = newOpt
      )
    }
  }

  case class ParentTag(name: String, requiredAttr: List[(String, String)], conditions: Option[Condition])
  case class Condition(childCount: String)

  object TagAttributeRules {
    def empty = TagAttributeRules(Set.empty, Seq.empty, None, None)
  }

  def convertJsonStrToAttributeRules(jsonStr: String): Map[String, TagAttributeRules] = {
    implicit val formats = org.json4s.DefaultFormats + new EnumNameSerializer(TagAttributes)

    (parse(jsonStr) \ "attributes")
      .extract[JObject]
      .obj
      .map {
        case (fieldName, fieldValue) =>
          fieldName -> fieldValue.extract[TagAttributeRules]
      }
      .toMap
  }
}

object TagAttributes extends Enumeration {
  val DataUrl = Value("data-url")
  val DataAlt = Value("data-alt")
  val DataSize = Value("data-size")
  val DataAlign = Value("data-align")
  val DataWidth = Value("data-width")
  val DataHeight = Value("data-height")
  val DataPlayer = Value("data-player")
  val DataMessage = Value("data-message")
  val DataCaption = Value("data-caption")
  val DataAccount = Value("data-account")
  val DataVideoId = Value("data-videoid")
  val DataResource = Value("data-resource")
  val DataLinkText = Value("data-link-text")
  val DataOpenIn = Value("data-open-in")
  val DataContentId = Value("data-content-id")
  val DataContentType = Value("data-content-type")
  val DataNRKVideoId = Value("data-nrk-video-id")
  val DataResource_Id = Value("data-resource_id")
  val DataTitle = Value("data-title")
  val DataType = Value("data-type")
  val DataYear = Value("data-year")
  val DataEdition = Value("data-edition")
  val DataPublisher = Value("data-publisher")
  val DataAuthors = Value("data-authors")
  val DataArticleId = Value("data-article-id")
  val DataPath = Value("data-path")
  val DataFormat = Value("data-code-format")
  val DataContent = Value("data-code-content")
  val DataDisplay = Value("data-display")

  val DataUpperLeftY = Value("data-upper-left-y")
  val DataUpperLeftX = Value("data-upper-left-x")
  val DataLowerRightY = Value("data-lower-right-y")
  val DataLowerRightX = Value("data-lower-right-x")
  val DataFocalX = Value("data-focal-x")
  val DataFocalY = Value("data-focal-y")

  val XMLNsAttribute = Value("xmlns")

  val Href = Value("href")
  val Title = Value("title")
  val Align = Value("align")
  val Valign = Value("valign")
  val Target = Value("target")
  val Rel = Value("rel")
  val Class = Value("class")
  val Lang = Value("lang")
  val Rowspan = Value("rowspan")
  val Colspan = Value("colspan")
  val Name = Value("name")
  val Start = Value("start")
  val Style = Value("style")
  val Span = Value("span")
  val Id = Value("id")
  val Scope = Value("scope")
  val Headers = Value("headers")

  private[validation] def getOrCreate(s: String): TagAttributes.Value = {
    valueOf(s).getOrElse(Value(s))
  }

  def all: Set[String] = TagAttributes.values.map(_.toString)

  def valueOf(s: String): Option[TagAttributes.Value] = {
    TagAttributes.values.find(_.toString == s)
  }
}
