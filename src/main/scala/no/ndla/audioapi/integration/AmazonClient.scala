/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import com.amazonaws.services.s3.AmazonS3Client

trait AmazonClient {
  val amazonClient: AmazonS3Client
  val storageName: String
}
