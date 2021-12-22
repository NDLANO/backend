/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import com.amazonaws.services.s3.AmazonS3

trait AmazonClient {
  val amazonClient: AmazonS3
}
