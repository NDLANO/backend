/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import no.ndla.audioapi.AudioApiProperties
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http

trait MigrationApiClient {
  this: NdlaClient =>

  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    val audioMetadataEndpoint = s"${AudioApiProperties.MigrationHost}/contents/audiometa/:audio_id"

    def getAudioMetaData(audioNid: String): Try[MigrationAudioMeta] = {
      ndlaClient.fetch[MigrationAudioMeta](
        Http(audioMetadataEndpoint.replace(":audio_id", audioNid)),
        Some(AudioApiProperties.MigrationUser), Some(AudioApiProperties.MigrationPassword))
    }
  }
}

case class MigrationAudioMeta(nid: String, tnid: String, title: String, fileName: String, url: String, mimeType: String, fileSize: String)
