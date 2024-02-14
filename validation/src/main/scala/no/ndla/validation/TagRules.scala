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
  case object LIST    extends AttributeType
}

sealed abstract class TagAttribute(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}
object TagAttribute extends Enum[TagAttribute] with CirceEnum[TagAttribute] {
  val values: IndexedSeq[TagAttribute] = findValues

  case object Accent               extends TagAttribute("accent")
  case object AccentUnder          extends TagAttribute("accentunder")
  case object ActionType           extends TagAttribute("actiontype")
  case object Align                extends TagAttribute("align")
  case object AlignmentScope       extends TagAttribute("alignmentscope")
  case object AltImg               extends TagAttribute("altimg")
  case object AltImgHeight         extends TagAttribute("altimg-height")
  case object AltImgValign         extends TagAttribute("altimg-valign")
  case object AltImgWidth          extends TagAttribute("altimg-width")
  case object AltText              extends TagAttribute("alttext")
  case object Bevelled             extends TagAttribute("bevelled")
  case object Cd                   extends TagAttribute("cd")
  case object CharAlign            extends TagAttribute("charalign")
  case object CharSpacing          extends TagAttribute("charspacing")
  case object Class                extends TagAttribute("class")
  case object Close                extends TagAttribute("close")
  case object Colspan              extends TagAttribute("colspan")
  case object ColumnAlign          extends TagAttribute("columnalign")
  case object ColumnLines          extends TagAttribute("columnlines")
  case object ColumnSpacing        extends TagAttribute("columnspacing")
  case object ColumnSpan           extends TagAttribute("columnspan")
  case object ColumnWidth          extends TagAttribute("columnwidth")
  case object CrossOut             extends TagAttribute("crossout")
  case object DecimalPoint         extends TagAttribute("decimalpoint")
  case object DefinitionURL        extends TagAttribute("definitionURL")
  case object DenomAlign           extends TagAttribute("denomalign")
  case object Depth                extends TagAttribute("depth")
  case object Dir                  extends TagAttribute("dir")
  case object Display              extends TagAttribute("display")
  case object DisplayStyle         extends TagAttribute("displaystyle")
  case object Edge                 extends TagAttribute("edge")
  case object Encoding             extends TagAttribute("encoding")
  case object EqualColumns         extends TagAttribute("equalcolumns")
  case object EqualRows            extends TagAttribute("equalrows")
  case object Fence                extends TagAttribute("fence")
  case object Form                 extends TagAttribute("form")
  case object Frame                extends TagAttribute("frame")
  case object FrameSpacing         extends TagAttribute("framespacing")
  case object GroupAlign           extends TagAttribute("groupalign")
  case object Headers              extends TagAttribute("headers")
  case object Height               extends TagAttribute("height")
  case object Href                 extends TagAttribute("href")
  case object Id                   extends TagAttribute("id")
  case object IndentAlign          extends TagAttribute("indentalign")
  case object IndentAlignFirst     extends TagAttribute("indentalignfirst")
  case object IndentAlignLast      extends TagAttribute("indentalignlast")
  case object IndentShift          extends TagAttribute("indentshift")
  case object IndentShiftFirst     extends TagAttribute("indentshiftfirst")
  case object IndentShiftLast      extends TagAttribute("indentshiftlast")
  case object IndentTarget         extends TagAttribute("indenttarget")
  case object InfixLinebreakStyle  extends TagAttribute("infixlinebreakstyle")
  case object LQuote               extends TagAttribute("lquote")
  case object LSpace               extends TagAttribute("lspace")
  case object Lang                 extends TagAttribute("lang")
  case object LargeOp              extends TagAttribute("largeop")
  case object Length               extends TagAttribute("length")
  case object LineLeading          extends TagAttribute("lineleading")
  case object LineThickness        extends TagAttribute("linethickness")
  case object Linebreak            extends TagAttribute("linebreak")
  case object LinebreakMultichar   extends TagAttribute("linebreakmultchar")
  case object LinebreakStyle       extends TagAttribute("linebreakstyle")
  case object Location             extends TagAttribute("location")
  case object LongDivStyle         extends TagAttribute("longdivstyle")
  case object MathBackground       extends TagAttribute("mathbackground")
  case object MathColor            extends TagAttribute("mathcolor")
  case object MathSize             extends TagAttribute("mathsize")
  case object MathVariant          extends TagAttribute("mathvariant")
  case object MaxSize              extends TagAttribute("maxsize")
  case object MinLabelSpacing      extends TagAttribute("minlabelspacing")
  case object MinSize              extends TagAttribute("minsize")
  case object MovableLimits        extends TagAttribute("movablelimits")
  case object Name                 extends TagAttribute("name")
  case object Notation             extends TagAttribute("notation")
  case object NumAlign             extends TagAttribute("numalign")
  case object Open                 extends TagAttribute("open")
  case object Overflow             extends TagAttribute("overflow")
  case object Position             extends TagAttribute("position")
  case object RQuote               extends TagAttribute("rquote")
  case object RSpace               extends TagAttribute("rspace")
  case object Rel                  extends TagAttribute("rel")
  case object RowAlign             extends TagAttribute("rowalign")
  case object RowLines             extends TagAttribute("rowlines")
  case object RowSpacing           extends TagAttribute("rowspacing")
  case object Rowspan              extends TagAttribute("rowspan")
  case object Scope                extends TagAttribute("scope")
  case object ScriptLevel          extends TagAttribute("scriptlevel")
  case object ScriptMinSize        extends TagAttribute("scriptminsize")
  case object ScriptSizeMultiplier extends TagAttribute("scriptsizemultiplier")
  case object Selection            extends TagAttribute("selection")
  case object Separator            extends TagAttribute("separator")
  case object Separators           extends TagAttribute("separators")
  case object Shift                extends TagAttribute("shift")
  case object Side                 extends TagAttribute("side")
  case object Span                 extends TagAttribute("span")
  case object Src                  extends TagAttribute("src")
  case object StackAlign           extends TagAttribute("stackalign")
  case object Start                extends TagAttribute("start")
  case object Stretchy             extends TagAttribute("stretchy")
  case object Style                extends TagAttribute("style")
  case object SubScriptShift       extends TagAttribute("subscriptshift")
  case object SupScriptShift       extends TagAttribute("supscriptshift")
  case object Symmetric            extends TagAttribute("symmetric")
  case object Target               extends TagAttribute("target")
  case object Title                extends TagAttribute("title")
  case object VOffset              extends TagAttribute("voffset")
  case object Valign               extends TagAttribute("valign")
  case object Width                extends TagAttribute("width")
  case object XLinkHref            extends TagAttribute("xlink:href")
  case object XMLNsAttribute       extends TagAttribute("xmlns")

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
  case object DataCodeFormat          extends TagAttribute("data-code-format")
  case object DataColumns             extends TagAttribute("data-columns")
  case object DataContent             extends TagAttribute("data-code-content")
  case object DataContentId           extends TagAttribute("data-content-id")
  case object DataContentType         extends TagAttribute("data-content-type")
  case object DataDate                extends TagAttribute("data-date")
  case object DataDescription         extends TagAttribute("data-description")
  case object DataDescriptionLanguage extends TagAttribute("data-description-language")
  case object DataDisclaimer          extends TagAttribute("data-disclaimer")
  case object DataDisplay             extends TagAttribute("data-display")
  case object DataEdition             extends TagAttribute("data-edition")
  case object DataEmail               extends TagAttribute("data-email")
  case object DataExampleIds          extends TagAttribute("data-example-ids")
  case object DataExampleLangs        extends TagAttribute("data-example-langs")
  case object DataFocalX              extends TagAttribute("data-focal-x")
  case object DataFocalY              extends TagAttribute("data-focal-y")
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
