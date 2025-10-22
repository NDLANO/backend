/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.draft.DraftStatus.PUBLISHED
import no.ndla.draftapi.model.api
import no.ndla.common.model.domain.getNextRevision

object DraftUtil {

  /** Returns fields to publish _if_ partial-publishing requirements are satisfied, otherwise returns empty set. */
  def shouldPartialPublish(existingArticle: Option[Draft], changedArticle: Draft): Set[api.PartialArticleFieldsDTO] = {
    val isPublished = changedArticle.status.current == PUBLISHED || changedArticle.status.other.contains(PUBLISHED)

    if (isPublished) {
      val changedFields = existingArticle
        .map(e => api.PartialArticleFieldsDTO.values.flatMap(field => compareField(field, e, changedArticle)))
        .getOrElse(api.PartialArticleFieldsDTO.values)

      changedFields.toSet
    } else {
      Set.empty
    }
  }

  private def compareField(
      field: api.PartialArticleFieldsDTO,
      old: Draft,
      changed: Draft,
  ): Option[api.PartialArticleFieldsDTO] = {
    import api.PartialArticleFieldsDTO.*
    val shouldInclude = field match {
      case `availability`    => old.availability != changed.availability
      case `grepCodes`       => old.grepCodes != changed.grepCodes
      case `relatedContent`  => old.relatedContent != changed.relatedContent
      case `tags`            => old.tags.sorted != changed.tags.sorted
      case `metaDescription` => old.metaDescription.sorted != changed.metaDescription.sorted
      case `license`         => old.copyright.flatMap(_.license) != changed.copyright.flatMap(_.license)
      case `revisionDate`    => compareRevisionDates(old, changed)
      case `published`       => old.published != changed.published
    }

    Option.when(shouldInclude)(field)
  }

  /** Compares articles to check whether earliest not-revised revision date has changed since that is the only one
    * article-api cares about.
    */
  private def compareRevisionDates(oldArticle: Draft, newArticle: Draft): Boolean = {
    oldArticle.revisionMeta.getNextRevision != newArticle.revisionMeta.getNextRevision
  }
}
