/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.amazonaws.services.s3.AmazonS3

trait AmazonClient {
  val amazonClient: AmazonS3
}
