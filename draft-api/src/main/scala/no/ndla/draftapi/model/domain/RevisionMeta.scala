/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import enumeratum._

import java.time.LocalDateTime

case class RevisionMeta(
    revisionDate: LocalDateTime,
    note: String,
    status: RevisionStatus
) {
  def toRevised: RevisionMeta = this.copy(status = RevisionStatus.Revised)
}

object RevisionMeta {
  def default: Seq[RevisionMeta] = Seq.empty
}

sealed abstract class RevisionStatus(override val entryName: String) extends EnumEntry

object RevisionStatus extends Enum[RevisionStatus] {
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
