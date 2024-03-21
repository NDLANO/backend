/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import io.circe.{Decoder, Encoder}

object UserRole extends Enumeration {
  val EMPLOYEE: UserRole.Value = Value("employee")
  val STUDENT: UserRole.Value  = Value("student")

  def valueOf(s: String): Option[FolderStatus.Value]         = FolderStatus.values.find(_.toString == s)
  def valueOf(s: Option[String]): Option[FolderStatus.Value] = s.flatMap(valueOf)

  implicit val encoder: Encoder[UserRole.Value] = Encoder.encodeEnumeration(UserRole)
  implicit val decoder: Decoder[UserRole.Value] = Decoder.decodeEnumeration(UserRole)

}
