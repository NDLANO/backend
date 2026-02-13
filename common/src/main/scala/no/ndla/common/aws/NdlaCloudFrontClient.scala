/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.aws

import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.{CreateInvalidationRequest, InvalidationBatch, Paths}

import java.util.UUID
import scala.jdk.CollectionConverters._

class NdlaCloudFrontClient {

  lazy val client: CloudFrontClient = {
    val builder = CloudFrontClient.builder()
    builder.build()
  }

  def createInvalidation(distributionId: Option[String], paths: Seq[String]): Unit = {
    val distId = distributionId.getOrElse {
      throw new IllegalArgumentException("Distribution ID must be provided for CloudFront invalidation.")
    }

    val invalidationBatch = InvalidationBatch
      .builder()
      .paths(Paths.builder().items(paths.asJava).quantity(paths.size).build())
      .callerReference(UUID.randomUUID().toString)
      .build()

    val request = CreateInvalidationRequest
      .builder()
      .distributionId(distId)
      .invalidationBatch(invalidationBatch)
      .build()

    client.createInvalidation(request).invalidation(): Unit
  }
}
