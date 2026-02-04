package no.ndla.myndlaapi.model.domain

import no.ndla.common.model.NDLADate

case class InactiveUserCleanupResult(id: Long, numCleanup: Int, numEmailed: Int, lastCleanupDate: NDLADate)
