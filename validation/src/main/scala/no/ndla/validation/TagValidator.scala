/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import cats.implicits._
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationMessage
import no.ndla.validation.AttributeType._
import no.ndla.validation.TagRules.{ChildrenRule, TagAttributeRules}
import org.jsoup.nodes.{Element, Node}

import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

object TagValidator {

  def validate(
      fieldName: String,
      content: String,
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {

    val document = HtmlTagRules.stringToJsoupDocument(content)
    document
      .select("*")
      .asScala
      .flatMap(tag => {
        if (tag.tagName == EmbedTagName) validateEmbedTag(fieldName, tag, requiredToOptional)
        else validateHtmlTag(fieldName, tag)
      })
      .toList
  }

  private def validateHtmlTag(fieldName: String, html: Element): Seq[ValidationMessage] = {
    val tagName = html.tagName
    if (!HtmlTagRules.isTagValid(tagName)) {
      return Seq.empty
    }

    val allAttributesOnTag  = html.attributes().asScala.map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributesUsed = getLegalAttributesUsed(allAttributesOnTag, tagName)
    val validationErrors    = attributesAreLegal(fieldName, allAttributesOnTag, tagName)

    val tagAttributeRules = HtmlTagRules.tagAttributesForTagType(html.tagName).getOrElse(TagAttributeRules.empty)

    val missingAttributes = getMissingAttributes(tagAttributeRules.required, legalAttributesUsed.keys.toSet)
    val missingErrors = missingAttributes
      .map(missingAttributes =>
        ValidationMessage(
          fieldName,
          s"$tagName must contain the following attributes: ${tagAttributeRules.required.map(_.name).mkString(", ")}. " +
            s"Optional attributes are: ${tagAttributeRules.optional.map(_.name).mkString(", ")}. " +
            s"Missing: ${missingAttributes.mkString(", ")}"
        )
      )
      .toList
    val partialErrorMessage = s"A $tagName HTML tag"

    val optionalErrors =
      verifyOptionals(fieldName, tagAttributeRules, legalAttributesUsed.keys.toSet, partialErrorMessage)

    validationErrors ++ missingErrors ++ optionalErrors
  }

  private def validateEmbedTag(
      fieldName: String,
      embed: Element,
      requiredToOptional: Map[String, Seq[String]]
  ): Seq[ValidationMessage] = {
    if (embed.tagName != EmbedTagName)
      return Seq()

    val allAttributesOnTag  = embed.attributes().asScala.map(attr => attr.getKey -> attr.getValue).toMap
    val legalAttributesUsed = getLegalAttributesUsed(allAttributesOnTag, EmbedTagName)

    val (resourceType, tagAttributeRules) = getRules(fieldName, legalAttributesUsed, requiredToOptional) match {
      case Left(validationError) => return Seq(validationError)
      case Right(typeAndRules)   => typeAndRules
    }

    val validationErrors = attributesAreLegal(fieldName, allAttributesOnTag, EmbedTagName) ++
      verifyEmbedAttributes(fieldName, tagAttributeRules, legalAttributesUsed) ++
      verifyAttributeResource(fieldName, tagAttributeRules, resourceType, legalAttributesUsed) ++
      verifyParent(fieldName, resourceType, embed) ++
      verifyRequiredOptional(fieldName, legalAttributesUsed, resourceType, tagAttributeRules) ++
      validateChildren(fieldName, resourceType, tagAttributeRules, embed)

    validationErrors
  }

  private def getRules(
      fieldName: String,
      attributes: Map[TagAttribute, String],
      requiredToOptional: Map[String, Seq[String]]
  ): Either[ValidationMessage, (ResourceType, TagAttributeRules)] = {
    val attributeKeys = attributes.keySet
    if (!attributeKeys.contains(TagAttribute.DataResource)) {
      return Left(
        ValidationMessage(
          fieldName,
          s"$EmbedTagName tags must contain a ${TagAttribute.DataResource} attribute"
        )
      )
    }

    if (!ResourceType.values.map(_.toString).contains(attributes(TagAttribute.DataResource))) {
      return Left(
        ValidationMessage(
          fieldName,
          s"The ${TagAttribute.DataResource} attribute can only contain one of the following values: ${ResourceType.values
              .mkString(", ")}"
        )
      )
    }

    val resourceType = ResourceType.withNameOption(attributes(TagAttribute.DataResource)).get
    val attributeRulesForTag = EmbedTagRules
      .attributesForResourceType(resourceType)
      .withOptionalRequired(requiredToOptional.getOrElse(resourceType.toString, Seq.empty))

    Right((resourceType, attributeRulesForTag))

  }

  private def verifyParent(
      fieldName: String,
      resourceType: ResourceType,
      embed: Element
  ): Seq[ValidationMessage] = {
    val attributeRulesForTag = EmbedTagRules.attributesForResourceType(resourceType)
    attributeRulesForTag.mustBeDirectChildOf.toSeq.flatMap(parentRule => {
      val parentEither = parentRule.conditions
        .map(checkParentConditions(fieldName, _, numDirectEqualSiblings(embed)))
        .getOrElse(Right(true))

      if (parentEither.getOrElse(true)) {
        val parent = embed.parent()
        val expectedButMissingParentAttributes = parentRule.requiredAttr.filterNot { case (attrKey, attrVal) =>
          parent.attr(attrKey) == attrVal
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
  }

  private def isSameEmbedType(embed: Element, n: Node): Boolean = {
    n.nodeName() == embed.tagName &&
    n.attr(TagAttribute.DataResource.toString) == embed.attr(TagAttribute.DataResource.toString)
  }

  /** Counts number of siblings that are next to the embed, with the same type.
    *
    * @param embed
    *   Embed tag to count direct siblings for
    * @return
    *   Number of direct equal typed (data-resource) siblings
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

    val equalAfterSiblings  = afterSiblings.takeWhile(isSameEmbedType(embed, _))
    val equalBeforeSiblings = beforeSiblings.takeWhile(isSameEmbedType(embed, _))

    1 + equalAfterSiblings.size + equalBeforeSiblings.size // Itself + Number of equal sibling nodes before + Number of equal sibling nodes after
  }

  /** Checks whether parentConditions are met and returns an either with Right(true) if they are met and Right(false) if
    * they are not. Either with Left suggests that the configuration for conditions were wrong.
    *
    * @param fieldName
    *   Name used in error message if check fails.
    * @param condition
    *   Condition that needs to be satisfied to validate parent
    * @param childCount
    *   Number of children the parent that is to be validated has
    * @return
    *   Either with Left with list of error messages (if the condition is wrongly specified) or a Right with a Boolean
    *   stating whether the condition is satisfied or not
    */
  private[validation] def checkParentConditions(
      fieldName: String,
      condition: TagRules.Condition,
      childCount: Int
  ): Either[Seq[ValidationMessage], Boolean] = {
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
                "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1')."
            )
          )
        )
    }
  }

  private def verifyRequiredOptional(
      fieldName: String,
      attributes: Map[TagAttribute, String],
      resourceType: ResourceType,
      attributeRulesForTag: TagAttributeRules
  ): Seq[ValidationMessage] = {
    val legalOptionals              = attributeRulesForTag.optional.map(f => f.name)
    val legalOptionalAttributesUsed = attributes.keySet.intersect(legalOptionals)

    if (attributeRulesForTag.mustContainOptionalAttribute && legalOptionalAttributesUsed.isEmpty) {
      List(
        ValidationMessage(
          fieldName,
          s"An Embed tag with data-resource '$resourceType' must contain at least one optional attribute"
        )
      )
    } else {
      List.empty
    }
  }

  private def validateChildren(
      fieldName: String,
      resourceType: ResourceType,
      tagRules: TagAttributeRules,
      embed: Element
  ): Option[ValidationMessage] = {
    val childrenRule = tagRules.children.getOrElse(ChildrenRule.default)
    if (childrenRule.required) {
      if (embed.childNodeSize() == 0) {
        return ValidationMessage(
          fieldName,
          s"$EmbedTagName with `data-resource=$resourceType` requires at least one child."
        ).some
      }

      val childrenTagNames  = embed.children().asScala.toList.map(_.tagName())
      val onlyValidChildren = childrenTagNames.forall(tag => childrenRule.allowedChildren.contains(tag))
      Option.when(!onlyValidChildren) {
        ValidationMessage(
          fieldName,
          s"$EmbedTagName tag with `data-resource=$resourceType` can only have the following children tags: [${childrenRule.allowedChildren
              .mkString(",")}]"
        )
      }
    } else {
      Option.when(embed.childNodeSize() != 0) {
        ValidationMessage(
          fieldName,
          s"$EmbedTagName tag with `data-resource=${resourceType.toString}` are not allowed to have children."
        )
      }
    }
  }

  private def attributesAreLegal(
      fieldName: String,
      attributes: Map[String, String],
      tagName: String
  ): List[ValidationMessage] = {
    val legalAttributeKeys    = HtmlTagRules.legalAttributesForTag(tagName)
    val legalAttributesForTag = HtmlTagRules.tagAttributesForTagType(tagName).getOrElse(TagAttributeRules.empty)

    val illegalAttributesUsed: Set[String] = attributes.keySet.diff(legalAttributeKeys)
    val legalOptionalAttributesUsed =
      attributes.keySet.intersect(legalAttributesForTag.optional.map(_.name.entryName))

    val illegalTagsError = if (illegalAttributesUsed.nonEmpty) {
      List(
        ValidationMessage(
          fieldName,
          s"An HTML tag '$tagName' contains an illegal attribute(s) '${illegalAttributesUsed
              .mkString(", ")}'. Allowed attributes are ${legalAttributeKeys.mkString(", ")}"
        )
      )
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

  private def verifyEmbedAttributes(
      fieldName: String,
      attributeRulesForTag: TagAttributeRules,
      attributes: Map[TagAttribute, String]
  ): Option[ValidationMessage] = {
    val attributesWithIllegalHtml = attributes.toList
      .filter { case (attribute, value) =>
        attributeRulesForTag
          .field(attribute)
          .exists(field => TextValidator.validate(attribute.toString, value, field.validation.allowedHtml).nonEmpty)
      }
      .toMap
      .keySet

    if (attributesWithIllegalHtml.nonEmpty) {
      Some(
        ValidationMessage(
          fieldName,
          s"HTML tag '$EmbedTagName' contains attributes with HTML: ${attributesWithIllegalHtml.mkString(", ")}"
        )
      )
    } else {
      None
    }
  }

  private def verifyAttributeResource(
      fieldName: String,
      attributeRulesForTag: TagAttributeRules,
      resourceType: ResourceType,
      attributes: Map[TagAttribute, String]
  ): Seq[ValidationMessage] = {
    val partialErrorMessage = s"An $EmbedTagName HTML tag with ${TagAttribute.DataResource}=$resourceType"

    verifyEmbedTagBasedOnResourceType(fieldName, attributeRulesForTag, attributes, resourceType) ++
      verifyOptionals(fieldName, attributeRulesForTag, attributes.keySet, partialErrorMessage) ++
      verifyAttributeFormat(fieldName, attributeRulesForTag, attributes, partialErrorMessage)
  }

  private def verifyEmbedTagBasedOnResourceType(
      fieldName: String,
      attrRules: TagAttributeRules,
      actualAttributes: Map[TagAttribute, String],
      resourceType: ResourceType
  ): Seq[ValidationMessage] = {
    val requiredAttrs     = attrRules.required
    val missingAttributes = getMissingAttributes(requiredAttrs, actualAttributes.keySet)
    val illegalAttributes = getIllegalAttributes(actualAttributes.keySet, attrRules.fields)

    val partialErrorMessage = s"An $EmbedTagName HTML tag with ${TagAttribute.DataResource}=$resourceType"

    val missingErrors = missingAttributes
      .map(missingAttributes =>
        ValidationMessage(
          fieldName,
          s"$partialErrorMessage must contain the following attributes: ${requiredAttrs.map(_.name).mkString(", ")}. " +
            s"Optional attributes are: ${attrRules.optional.map(_.name).mkString(", ")}. " +
            s"Missing: ${missingAttributes.mkString(", ")}"
        )
      )
      .toList

    val illegalErrors = illegalAttributes
      .map(illegalAttributes =>
        ValidationMessage(
          fieldName,
          s"$partialErrorMessage can not contain any of the following attributes: ${illegalAttributes.mkString(", ")}"
        )
      )

    missingErrors ++ illegalErrors
  }

  private def verifyOptionals(
      fieldName: String,
      attrsRules: TagAttributeRules,
      actualAttributes: Set[TagAttribute],
      partialErrorMessage: String
  ): Seq[ValidationMessage] = {
    val usedOptionalFields = attrsRules.optional.filter(f => actualAttributes.contains(f.name))
    val neededOptionals    = usedOptionalFields.flatMap(f => f.validation.mustCoexistWith)
    val missingOptionals   = neededOptionals.diff(actualAttributes)

    buildUnmatchedErrors(
      fieldName,
      missingOptionals,
      usedOptionalFields.map(f => f.name),
      attrsRules,
      partialErrorMessage
    )
  }

  private def buildUnmatchedErrors(
      fieldName: String,
      missingTags: Set[TagAttribute],
      usedOptionals: Set[TagAttribute],
      attrsRules: TagAttributeRules,
      partialErrorMessage: String
  ): Seq[ValidationMessage] =
    if (missingTags.isEmpty) { Seq.empty }
    else {
      missingTags.toSeq
        .map(tag => {
          val optionalField = attrsRules.optional.filter(_.name.eq(tag))
          val optionGroup   = optionalField.flatMap(f => f.validation.mustCoexistWith :+ f.name)
          val groupErrors = s"${optionGroup.mkString(",")} (Missing: ${optionGroup.diff(usedOptionals).mkString(",")})"
          ValidationMessage(
            fieldName,
            s"$partialErrorMessage must contain all or none of the attributes in the optional attribute group: ($groupErrors)"
          )
        })
        .distinct
    }

  private def verifyAttributeFormat(
      fieldName: String,
      tagAttributeRules: TagAttributeRules,
      usedAttributes: Map[TagAttribute, String],
      partialErrorMessage: String
  ): Seq[ValidationMessage] =
    usedAttributes.flatMap { case (key, value) =>
      tagAttributeRules
        .field(key)
        .flatMap(f =>
          f.validation.dataType match {
            case BOOLEAN => validateBooleanField(fieldName, partialErrorMessage, key, value, f)
            case EMAIL   => validateEmailField(fieldName, partialErrorMessage, key, value, f)
            case NUMBER  => validateNumberField(fieldName, partialErrorMessage, key, value, f)
            case STRING  => None
            case URL     => validateUrlField(fieldName, partialErrorMessage, key, value, f)
          }
        )
    }.toSeq

  private def validateBooleanField(
      fieldName: String,
      partialErrorMessage: String,
      key: TagAttribute,
      value: String,
      field: TagRules.Field
  ): Option[ValidationMessage] = {
    value.toBooleanOption match {
      case Some(_)                                             => None
      case None if !field.validation.required && value.isEmpty => None
      case None =>
        Some(
          ValidationMessage(
            fieldName,
            s"$partialErrorMessage and attribute $key=$value must have a valid boolean value."
          )
        )
    }
  }

  private def validateEmailField(
      fieldName: String,
      partialErrorMessage: String,
      key: TagAttribute,
      value: String,
      field: TagRules.Field
  ): Option[ValidationMessage] = {
    val emailRegex =
      "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    value.matches(emailRegex) match {
      case true                                                 => None
      case false if !field.validation.required && value.isEmpty => None
      case false =>
        Some(
          ValidationMessage(
            fieldName,
            s"$partialErrorMessage and $key=$value must be a valid email address."
          )
        )

    }
  }

  private def validateNumberField(
      fieldName: String,
      partialErrorMessage: String,
      key: TagAttribute,
      value: String,
      field: TagRules.Field
  ): Option[ValidationMessage] = {
    value.toDoubleOption match {
      case Some(_)                                             => None
      case None if !field.validation.required && value.isEmpty => None
      case None =>
        Some(
          ValidationMessage(
            fieldName,
            s"$partialErrorMessage and attribute $key=$value must have a valid numeric value."
          )
        )
    }
  }

  private def validateUrlField(
      fieldName: String,
      partialErrorMessage: String,
      key: TagAttribute,
      value: String,
      field: TagRules.Field
  ): Option[ValidationMessage] = {
    val domainRegex =
      "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)"

    if (value.matches(domainRegex)) {
      if (field.validation.allowedDomains.isEmpty) None
      else {
        val urlHost               = value.hostOption.map(_.toString).getOrElse("")
        val urlMatchesValidDomain = field.validation.allowedDomains.exists(domain => urlHost.matches(domain))
        if (urlMatchesValidDomain) None
        else
          Some(
            ValidationMessage(
              fieldName,
              s"$partialErrorMessage and $key=$value can only contain urls from the following domains: ${field.validation.allowedDomains
                  .mkString(", ")}"
            )
          )
      }
    } else {
      Some(
        ValidationMessage(
          fieldName,
          s"$partialErrorMessage and $key=$value must be a valid url address."
        )
      )
    }
  }

  private def getMissingAttributes(
      requiredAttributes: Set[TagRules.Field],
      attributeKeys: Set[TagAttribute]
  ): Option[Set[TagAttribute]] = {
    val missing = requiredAttributes.map(_.name) diff attributeKeys
    missing.headOption.map(_ => missing)
  }

  private def getIllegalAttributes(
      usedAttributes: Set[TagAttribute],
      legalFields: Set[TagRules.Field]
  ): Option[Set[TagAttribute]] = {
    val illegal = usedAttributes.filterNot(legalFields.map(f => f.name))
    if (illegal.isEmpty) None else Some(illegal)
  }

  private def getLegalAttributesUsed(
      allAttributes: Map[String, String],
      tagName: String
  ): Map[TagAttribute, String] = {
    val legalAttributeKeys = HtmlTagRules.legalAttributesForTag(tagName)

    allAttributes
      .filter { case (key, _) => legalAttributeKeys.contains(key) }
      .map { case (key, value) =>
        TagAttribute.withName(key) -> value
      }
  }

}
