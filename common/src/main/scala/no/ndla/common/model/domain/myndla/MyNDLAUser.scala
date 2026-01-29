/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.myndla

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Availability

case class MyNDLAUser(
    id: Long,
    feideId: String,
    favoriteSubjects: Seq[String],
    userRole: UserRole,
    lastUpdated: NDLADate,
    organization: String,
    groups: Seq[MyNDLAGroup],
    username: String,
    displayName: String,
    email: String,
    arenaEnabled: Boolean,
    lastSeen: NDLADate,
) {
  // Keeping FEIDE and our data in sync
  def wasUpdatedLast24h: Boolean = NDLADate.now().isBefore(lastUpdated.minusSeconds(10))

  def isStudent: Boolean = userRole == UserRole.STUDENT
  def isTeacher: Boolean = userRole == UserRole.EMPLOYEE

  def availabilities: Seq[Availability] = {
    if (this.isTeacher) {
      Seq(Availability.everyone, Availability.teacher)
    } else {
      Seq(Availability.everyone)
    }
  }
}

object MyNDLAUser {
  implicit val encoder: Encoder[MyNDLAUser] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAUser] = deriveDecoder
}
