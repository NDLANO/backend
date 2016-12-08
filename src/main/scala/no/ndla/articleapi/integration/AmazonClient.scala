/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.integration

import com.amazonaws.services.s3.AmazonS3

trait AmazonClient {
  val amazonClient: AmazonS3
  val attachmentStorageName: String
}
