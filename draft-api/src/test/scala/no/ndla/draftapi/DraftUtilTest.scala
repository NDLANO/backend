/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import no.ndla.common.model.domain.{Description, Status}
import no.ndla.common.model.domain.draft.DraftStatus.{PLANNED, PUBLISHED}
import no.ndla.draftapi.DraftUtil.shouldPartialPublish
import no.ndla.draftapi.model.api.PartialArticleFieldsDTO

class DraftUtilTest extends UnitSuite with TestEnvironment {
  test("shouldPartialPublish return empty-set if articles are equal") {
    val nnMeta = Description("Meta nn", "nn")
    val nbMeta = Description("Meta nb", "nb")

    val article1 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    val article2 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    shouldPartialPublish(Some(article1), article2) should be(Set.empty)

    val article3 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    val article4 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nbMeta, nnMeta)
    )
    shouldPartialPublish(Some(article3), article4) should be(Set.empty)
  }

  test("shouldPartialPublish returns set of changed fields") {
    val article1 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(
        Description("Meta nn", "nn"),
        Description("Meta nb", "nb")
      ),
      grepCodes = Seq(
        "KE123"
      )
    )

    val article2 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(
        Description("Ny Meta nn", "nn"),
        Description("Meta nb", "nb")
      ),
      grepCodes = Seq("KE123", "KE456")
    )

    shouldPartialPublish(Some(article1), article2) should be(
      Set(
        PartialArticleFieldsDTO.metaDescription,
        PartialArticleFieldsDTO.grepCodes
      )
    )
  }
}
