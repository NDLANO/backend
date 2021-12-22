/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import io.lemonlabs.uri.dsl._
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.validation.TagRules.TagAttributeRules
import org.jsoup.nodes.{Element, Node}

import scala.collection.JavaConverters._ // TODO: Replace with `import scala.jdk.CollectionConverters._` when removing 2.12 support
import scala.util.{Success, Try}

class TagValidator {

  def validate(
      fieldName: String,
      content: String,
      validateParent: Boolean = true,
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {

    val document = HtmlTagRules.stringToJsoupDocument(content)
    document
      .select("*")
      .asScala
      .flatMap(tag => {
        if (tag.tagName == ResourceHtmlEmbedTag) validateEmbedTag(fieldName, tag, validateParent, requiredToOptional)
        else validateHtmlTag(fieldName, tag)
      })
      .toList

  }

  def validateHtmlTag(fieldName: String, html: Element): Seq[ValidationMessage] = {
    val tagName = html.tagName
    if (!HtmlTagRules.isTagValid(tagName)) {
      return Seq.empty
    }

    val allAttributesOnTag = html.attributes().asScala.map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributesUsed = getLegalAttributesUsed(allAttributesOnTag, tagName)
    val validationErrors = attributesAreLegal(fieldName, allAttributesOnTag, tagName)

    val legalAttributesForTag = HtmlTagRules.tagAttributesForTagType(html.tagName).getOrElse(TagAttributeRules.empty)

    val missingAttributes = getMissingAttributes(legalAttributesForTag.required, legalAttributesUsed.keys.toSet)
    val missingErrors = missingAttributes
      .map(
        missingAttributes =>
          ValidationMessage(
            fieldName,
            s"$tagName must contain the following attributes: ${legalAttributesForTag.required.mkString(",")}. " +
              s"Optional attributes are: ${legalAttributesForTag.optional.mkString(",")}. " +
              s"Missing: ${missingAttributes.mkString(",")}"
        ))
      .toList
    val partialErrorMessage = s"A $tagName HTML tag"

    val optionalErrors =
      verifyOptionals(fieldName, legalAttributesForTag, legalAttributesUsed.keys.toSet, partialErrorMessage)

    validationErrors ++ missingErrors ++ optionalErrors
  }

  private def validateEmbedTag(
      fieldName: String,
      embed: Element,
      validateParent: Boolean = true,
      requiredToOptional: Map[String, Seq[String]]
  ): Seq[ValidationMessage] = {
    if (embed.tagName != ResourceHtmlEmbedTag)
      return Seq()

    val allAttributesOnTag = embed.attributes().asScala.map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributes = getLegalAttributesUsed(allAttributesOnTag, ResourceHtmlEmbedTag)

    val validationErrors = attributesAreLegal(fieldName, allAttributesOnTag, ResourceHtmlEmbedTag) ++
      attributesContainsNoHtml(fieldName, legalAttributes) ++
      verifyAttributeResource(fieldName, legalAttributes, requiredToOptional) ++
      verifyParent(fieldName, legalAttributes, embed, validateParent) ++
      verifyRequiredOptional(fieldName, legalAttributes, embed)

    validationErrors
  }

  private def verifyParent(fieldName: String,
                           attributes: Map[TagAttributes.Value, String],
                           embed: Element,
                           validateParent: Boolean = true): Seq[ValidationMessage] = {
    if (validateParent) {
      attributes
        .get(TagAttributes.DataResource)
        .map(resourceTypeString => {
          ResourceType.valueOf(resourceTypeString) match {
            case Some(resourceType) =>
              val attributeRulesForTag = EmbedTagRules.attributesForResourceType(resourceType)
              attributeRulesForTag.mustBeDirectChildOf.toSeq.flatMap(parentRule => {
                val parentEither = parentRule.conditions
                  .map(checkParentConditions(fieldName, _, numDirectEqualSiblings(embed)))
                  .getOrElse(Right(true))

                if (parentEither.getOrElse(true)) {
                  val parent = embed.parent()
                  val expectedButMissingParentAttributes = parentRule.requiredAttr.filterNot {
                    case (attrKey, attrVal) => parent.attr(attrKey) == attrVal
                  }

                  if (parent.tagName() != parentRule.name || expectedButMissingParentAttributes.nonEmpty) {
                    val requiredAttributes = parentRule.requiredAttr
                      .map { case (key, value) => s"""$key="$value"""" }
                      .mkString(", ")
                    val messageString =
                      s"Embed tag with '${resourceType.toString}' requires a parent '${parentRule.name}', with attributes: '$requiredAttributes'"

                    Seq(ValidationMessage(fieldName, messageString)) ++ parentEither.left.getOrElse(Seq.empty)
                  } else { Seq.empty }
                } else { Seq.empty }
              })
            case _ =>
              Seq(ValidationMessage(fieldName, "Something went wrong when determining resourceType of embed"))
          }
        })
        .getOrElse(Seq.empty)
    } else {
      Seq.empty
    }
  }

  private def isSameEmbedType(embed: Element, n: Node): Boolean = {
    n.nodeName() == embed.tagName &&
    n.attr(TagAttributes.DataResource.toString) == embed.attr(TagAttributes.DataResource.toString)
  }

  /**
    * Counts number of siblings that are next to the embed, with the same type.
    *
    * @param embed Embed tag to count direct siblings for
    * @return Number of direct equal typed (data-resource) siblings
    */
  private[validation] def numDirectEqualSiblings(embed: Element): Int = {
    // Every sibling that is not whitespace
    val siblings = embed.parent
      .childNodes()
      .asScala
      .filterNot(n => n.outerHtml().replaceAll("\\s", "") == "")

    // Before siblings is reversed so we can use takeWhile
    // We use drop(1) on after siblings because splitAt includes the element that is split at
    val (beforeSiblings, afterSiblings) = siblings.splitAt(siblings.indexOf(embed)) match {
      case (bs, as) => (bs.reverse, as.drop(1))
    }

    val equalAfterSiblings = afterSiblings.takeWhile(isSameEmbedType(embed, _))
    val equalBeforeSiblings = beforeSiblings.takeWhile(isSameEmbedType(embed, _))

    1 + equalAfterSiblings.size + equalBeforeSiblings.size // Itself + Number of equal sibling nodes before + Number of equal sibling nodes after
  }

  /**
    * Checks whether parentConditions are met and returns an either with Right(true) if they are met and Right(false) if they are not.
    * Either with Left suggests that the configuration for conditions were wrong.
    *
    * @param fieldName Name used in error message if check fails.
    * @param condition Condition that needs to be satisfied to validate parent
    * @param childCount Number of children the parent that is to be validated has
    * @return Either with Left with list of error messages (if the condition is wrongly specified) or a Right with a Boolean stating whether the condition is satisfied or not
    */
  private[validation] def checkParentConditions(fieldName: String,
                                                condition: TagRules.Condition,
                                                childCount: Int): Either[Seq[ValidationMessage], Boolean] = {
    val noSpace = condition.childCount.replace(" ", "")
    // Remove operator character and attempt to turn number to Int
    Try(noSpace.replaceFirst("[<>=]", "").toInt) match {
      case Success(expectedChildNum) =>
        noSpace.charAt(0) match {
          case '>' => Right(childCount > expectedChildNum)
          case '<' => Right(childCount < expectedChildNum)
          case '=' => Right(childCount == expectedChildNum)
          case _   => Left(Seq(ValidationMessage(fieldName, "Could not find supported operator (<, > or =)")))
        }
      case _ =>
        Left(
          Seq(
            ValidationMessage(
              fieldName,
              "Parent condition block is invalid. " +
                "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1').")))
    }
  }

  private def verifyRequiredOptional(fieldName: String,
                                     attributes: Map[TagAttributes.Value, String],
                                     embed: Element): Seq[ValidationMessage] = {
    attributes
      .get(TagAttributes.DataResource)
      .map(resourceTypeString => {
        ResourceType.valueOf(resourceTypeString) match {
          case Some(resourceType) =>
            val attributeRulesForTag = EmbedTagRules.attributesForResourceType(resourceType)
            val legalOptionals = attributeRulesForTag.optional.flatten.toSet
            val legalOptionalAttributesUsed = attributes.keySet.intersect(legalOptionals)

            if (attributeRulesForTag.mustContainAtLeastOneOptionalAttribute && legalOptionalAttributesUsed.isEmpty) {
              List(
                ValidationMessage(
                  fieldName,
                  s"An Embed tag with data-resource '$resourceType' must contain at least one optional attribute"))
            } else {
              List.empty
            }
          case _ =>
            Seq(ValidationMessage(fieldName, "Something went wrong when determining resourceType of embed"))
        }
      })
      .getOrElse(Seq(
        ValidationMessage(fieldName,
                          s"Embed-tag did not contain any ${TagAttributes.DataResource.toString} attribute.")))
  }

  private def attributesAreLegal(fieldName: String,
                                 attributes: Map[String, String],
                                 tagName: String): List[ValidationMessage] = {

    val legalAttributeKeys = HtmlTagRules.legalAttributesForTag(tagName)
    val legalAttributesForTag = HtmlTagRules.tagAttributesForTagType(tagName).getOrElse(TagAttributeRules.empty)

    val illegalAttributesUsed: Set[String] = attributes.keySet.diff(legalAttributeKeys)
    val legalOptionalAttributesUsed =
      attributes.keySet.intersect(legalAttributesForTag.optional.flatten.map(_.toString).toSet)

    val illegalTagsError = if (illegalAttributesUsed.nonEmpty) {
      List(
        ValidationMessage(
          fieldName,
          s"An HTML tag '$tagName' contains an illegal attribute(s) '${illegalAttributesUsed
            .mkString(",")}'. Allowed attributes are ${legalAttributeKeys.mkString(",")}"
        ))
    } else {
      List.empty
    }

    val mustContainAttributesError =
      if (HtmlTagRules.tagMustContainAtLeastOneOptionalAttribute(tagName) && legalOptionalAttributesUsed.isEmpty) {
        List(ValidationMessage(fieldName, s"An HTML tag '$tagName' must contain at least one attribute"))
      } else {
        List.empty
      }

    illegalTagsError ++ mustContainAttributesError
  }

  private def attributesContainsNoHtml(fieldName: String,
                                       attributes: Map[TagAttributes.Value, String]): Option[ValidationMessage] = {
    val attributesWithHtml = attributes.toList
      .filter {
        case (_, value) =>
          new TextValidator(allowHtml = false).validate(fieldName, value).nonEmpty
      }
      .toMap
      .keySet

    if (attributesWithHtml.nonEmpty) {
      Some(
        ValidationMessage(
          fieldName,
          s"HTML tag '$ResourceHtmlEmbedTag' contains attributes with HTML: ${attributesWithHtml.mkString(",")}"))
    } else {
      None
    }
  }

  private def verifyAttributeResource(fieldName: String,
                                      attributes: Map[TagAttributes.Value, String],
                                      requiredToOptional: Map[String, Seq[String]]): Seq[ValidationMessage] = {
    val attributeKeys = attributes.keySet
    if (!attributeKeys.contains(TagAttributes.DataResource)) {
      return ValidationMessage(
        fieldName,
        s"$ResourceHtmlEmbedTag tags must contain a ${TagAttributes.DataResource} attribute") :: Nil
    }

    if (!ResourceType.all.contains(attributes(TagAttributes.DataResource))) {
      return Seq(
        ValidationMessage(
          fieldName,
          s"The ${TagAttributes.DataResource} attribute can only contain one of the following values: ${ResourceType.all
            .mkString(",")}"))
    }

    val resourceType = ResourceType.valueOf(attributes(TagAttributes.DataResource)).get
    val attributeRulesForTag = EmbedTagRules
      .attributesForResourceType(resourceType)
      .withOptionalRequired(requiredToOptional.get(resourceType.toString).getOrElse(Seq.empty))

    val partialErrorMessage = s"An $ResourceHtmlEmbedTag HTML tag with ${TagAttributes.DataResource}=$resourceType"

    verifyEmbedTagBasedOnResourceType(fieldName, attributeRulesForTag, attributeKeys, resourceType) ++
      verifyOptionals(fieldName, attributeRulesForTag, attributeKeys, partialErrorMessage) ++
      verifySourceUrl(fieldName, attributeRulesForTag, attributes, resourceType)
  }

  private def verifyEmbedTagBasedOnResourceType(fieldName: String,
                                                attrRules: TagAttributeRules,
                                                actualAttributes: Set[TagAttributes.Value],
                                                resourceType: ResourceType.Value): Seq[ValidationMessage] = {
    val missingAttributes = getMissingAttributes(attrRules.required, actualAttributes)
    val illegalAttributes = getMissingAttributes(actualAttributes, attrRules.all)

    val partialErrorMessage = s"An $ResourceHtmlEmbedTag HTML tag with ${TagAttributes.DataResource}=$resourceType"
    missingAttributes
      .map(
        missingAttributes =>
          ValidationMessage(
            fieldName,
            s"$partialErrorMessage must contain the following attributes: ${attrRules.required.mkString(",")}. " +
              s"Optional attributes are: ${attrRules.optional.mkString(",")}. " +
              s"Missing: ${missingAttributes.mkString(",")}"
        ))
      .toList ++
      illegalAttributes.map(illegalAttributes =>
        ValidationMessage(
          fieldName,
          s"$partialErrorMessage can not contain any of the following attributes: ${illegalAttributes.mkString(",")}"))
  }

  private def verifyOptionals(fieldName: String,
                              attrsRules: TagAttributeRules,
                              actualAttributes: Set[TagAttributes.Value],
                              partialErrorMessage: String): Seq[ValidationMessage] = {

    val usedOptionalAttr = actualAttributes.intersect(attrsRules.optional.flatten.toSet)
    val a = usedOptionalAttr
      .flatMap(attr => {
        val attrRuleGroup = attrsRules.optional.find(_.contains(attr))
        attrRuleGroup.map(attrRules =>
          verifyUsedAttributesAgainstAttrRules(fieldName, attrRules, usedOptionalAttr, partialErrorMessage))
      })
      .toSeq
    a.flatten
  }

  private def verifyUsedAttributesAgainstAttrRules(fieldName: String,
                                                   attrRules: Set[TagAttributes.Value],
                                                   usedOptionalAttrs: Set[TagAttributes.Value],
                                                   partialErrorMessage: String): Seq[ValidationMessage] = {
    val usedOptionalAttrsInCurrentGroup = usedOptionalAttrs.intersect(attrRules)
    usedOptionalAttrsInCurrentGroup.isEmpty match {
      case false if usedOptionalAttrsInCurrentGroup != attrRules =>
        val missingAttrs = attrRules.diff(usedOptionalAttrs).mkString(",")
        Seq(ValidationMessage(
          fieldName,
          s"$partialErrorMessage must contain all or none of the optional attributes (${attrRules.mkString(",")}). Missing $missingAttrs"))
      case _ => Seq.empty
    }
  }

  private def verifySourceUrl(fieldName: String,
                              attrs: TagAttributeRules,
                              usedAttributes: Map[TagAttributes.Value, String],
                              resourceType: ResourceType.Value): Seq[ValidationMessage] = {
    usedAttributes.get(TagAttributes.DataUrl) match {
      case Some(url)
          if attrs.validSrcDomains.nonEmpty && !attrs.validSrcDomains.get.exists(
            url.hostOption.getOrElse("").toString.matches) =>
        Seq(
          ValidationMessage(
            fieldName,
            s"An $ResourceHtmlEmbedTag HTML tag with ${TagAttributes.DataResource}=$resourceType can only contain ${TagAttributes.DataUrl} urls from the following domains: ${attrs.validSrcDomains
              .mkString(",")}"
          ))
      case _ => Seq.empty
    }
  }

  private def getMissingAttributes(requiredAttributes: Set[TagAttributes.Value],
                                   attributeKeys: Set[TagAttributes.Value]) = {
    val missing = requiredAttributes diff attributeKeys
    missing.headOption.map(_ => missing)
  }

  private def getLegalAttributesUsed(allAttributes: Map[String, String],
                                     tagName: String): Map[TagAttributes.Value, String] = {
    val legalAttributeKeys = HtmlTagRules.legalAttributesForTag(tagName)

    allAttributes
      .filter { case (key, _) => legalAttributeKeys.contains(key) }
      .map {
        case (key, value) =>
          TagAttributes.valueOf(key).getOrElse(TagAttributes.getOrCreate(key)) -> value
      }
  }

}
