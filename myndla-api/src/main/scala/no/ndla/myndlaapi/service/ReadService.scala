/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndla.repository.FolderRepository
import no.ndla.network.clients.FeideApiClient

trait ReadService {
  this: FeideApiClient with FolderRepository =>
  val readService: ReadService

  class ReadService {}
}
