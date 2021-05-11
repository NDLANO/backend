/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import io.lemonlabs.uri.typesafe.dsl.pathPartToUrlDsl
import no.ndla.audioapi.AudioApiProperties.{Environment, MigrationHost, MigrationPassword, MigrationUser}
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http

trait MigrationApiClient {
  this: NdlaClient =>
  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    val DBSource = "red"
    val AudioMetadataEndpoint = (s"$MigrationHost/audio/:audio_id" ? (s"db-source" -> s"$DBSource")).toString()
    val NodeDataEndpoint = (s"$MigrationHost/contents/:node_id" ? (s"db-source" -> s"$DBSource")).toString()

    def getAudioMetaData(audioNid: String): Try[Seq[MigrationAudioMeta]] = {
      ndlaClient.fetchWithBasicAuth[Seq[MigrationAudioMeta]](Http(AudioMetadataEndpoint.replace(":audio_id", audioNid)),
                                                             MigrationUser,
                                                             MigrationPassword)
    }

    def getNodeData(nid: String): Try[MigrationNodeData] = {
      ndlaClient.fetchWithBasicAuth[MigrationNodeData](Http(NodeDataEndpoint.replace(":node_id", nid)),
                                                       MigrationUser,
                                                       MigrationPassword)
    }
  }
}

case class MigrationNodeData(
    titles: Seq[MigrationTitle]
)

case class MigrationTitle(title: String, language: String)

case class MigrationAudioMeta(nid: String,
                              tnid: String,
                              title: String,
                              fileName: String,
                              url: String,
                              mimeType: String,
                              fileSize: String,
                              language: Option[String],
                              license: String,
                              authors: Seq[MigrationAuthor]) {
  def isMainNode = nid == tnid || tnid == "0"

}
case class MigrationAuthor(`type`: String, name: String)
