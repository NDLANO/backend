/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.effect.Sync
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder
import scala.language.higherKinds
import io.circe.generic.semiauto._
import io.circe.generic.auto._

case class UpdatedSubjectFrontPageData(name: Option[String],
                                       filters: Option[List[String]],
                                       externalId: Option[String],
                                       layout: Option[String],
                                       twitter: Option[String],
                                       facebook: Option[String],
                                       banner: Option[NewOrUpdateBannerImage],
                                       about: Option[Seq[NewOrUpdatedAboutSubject]],
                                       metaDescription: Option[Seq[NewOrUpdatedMetaDescription]],
                                       topical: Option[String],
                                       mostRead: Option[List[String]],
                                       editorsChoices: Option[List[String]],
                                       latestContent: Option[List[String]],
                                       goTo: Option[List[String]])

object UpdatedSubjectFrontPageData {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, UpdatedSubjectFrontPageData] =
    jsonOf[F, UpdatedSubjectFrontPageData](Sync[F], deriveDecoder[UpdatedSubjectFrontPageData])
}
