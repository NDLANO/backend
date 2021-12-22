/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import no.ndla.draftapi.model.domain.Availability

import java.util.Date

case class ArticleApiArticle(revision: Option[Int],
                             title: Seq[ArticleApiTitle],
                             content: Seq[ArticleApiContent],
                             copyright: Option[ArticleApiCopyright],
                             tags: Seq[ArticleApiTag],
                             requiredLibraries: Seq[ArticleApiRequiredLibrary],
                             visualElement: Seq[ArticleApiVisualElement],
                             introduction: Seq[ArticleApiIntroduction],
                             metaDescription: Seq[ArticleApiMetaDescription],
                             metaImage: Seq[ArticleApiMetaImage],
                             created: Date,
                             updated: Date,
                             updatedBy: String,
                             published: Date,
                             articleType: String,
                             grepCodes: Seq[String],
                             conceptIds: Seq[Long],
                             availability: Availability.Value,
                             relatedContent: Seq[RelatedContent])
case class ArticleApiTitle(title: String, language: String)
case class ArticleApiContent(content: String, language: String)
case class ArticleApiAuthor(`type`: String, name: String)
case class ArticleApiTag(tags: Seq[String], language: String)
case class ArticleApiRequiredLibrary(mediaType: String, name: String, url: String)
case class ArticleApiVisualElement(resource: String, language: String)
case class ArticleApiIntroduction(introduction: String, language: String)
case class ArticleApiMetaDescription(content: String, language: String)
case class ArticleApiMetaImage(imageId: String, altText: String, language: String)
case class ArticleApiCopyright(license: String,
                               origin: String,
                               creators: Seq[ArticleApiAuthor],
                               processors: Seq[ArticleApiAuthor],
                               rightsholders: Seq[ArticleApiAuthor],
                               agreementId: Option[Long],
                               validFrom: Option[Date],
                               validTo: Option[Date])
