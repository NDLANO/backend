/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import enumeratum._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate

import java.util.UUID

case class RevisionMeta(
    id: UUID,
    revisionDate: NDLADate,
    note: String,
    status: RevisionStatus
) {
  def toRevised: RevisionMeta = this.copy(status = RevisionStatus.Revised)
}

object RevisionMeta {
  implicit val encoder: Encoder[RevisionMeta] = deriveEncoder
  implicit val decoder: Decoder[RevisionMeta] = deriveDecoder

  def default: Seq[RevisionMeta] = Seq(
    RevisionMeta(
      UUID.randomUUID(),
      NDLADate.now().plusYears(5).withNano(0),
      "Automatisk revisjonsdato satt av systemet",
      RevisionStatus.NeedsRevision
    )
  )
}

sealed abstract class RevisionStatus(override val entryName: String) extends EnumEntry

object RevisionStatus extends Enum[RevisionStatus] with CirceEnum[RevisionStatus] {
  override def values: IndexedSeq[RevisionStatus] = findValues

  case object Revised       extends RevisionStatus("revised")
  case object NeedsRevision extends RevisionStatus("needs-revision")

  def fromStringOpt(s: String): Option[RevisionStatus] = {
    values.find(_.entryName.toLowerCase == s)
  }

  def fromString(s: String, fallback: RevisionStatus): RevisionStatus = {
    fromStringOpt(s).getOrElse(fallback)
  }

  def fromStringDefault(s: String): RevisionStatus = {
    fromString(s, NeedsRevision)
  }
}
