/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.effect.Sync
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder
import io.circe.generic.semiauto._
import io.circe.generic.auto._

case class NewSubjectFrontPageData(name: String,
                                   filters: Option[List[String]],
                                   externalId: Option[String],
                                   layout: String,
                                   twitter: Option[String],
                                   facebook: Option[String],
                                   banner: NewOrUpdateBannerImage,
                                   about: Seq[NewOrUpdatedAboutSubject],
                                   metaDescription: Seq[NewOrUpdatedMetaDescription],
                                   topical: Option[String],
                                   mostRead: Option[List[String]],
                                   editorsChoices: Option[List[String]],
                                   latestContent: Option[List[String]],
                                   goTo: Option[List[String]])

object NewSubjectFrontPageData {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, NewSubjectFrontPageData] =
    jsonOf[F, NewSubjectFrontPageData](Sync[F], deriveDecoder[NewSubjectFrontPageData])
}
