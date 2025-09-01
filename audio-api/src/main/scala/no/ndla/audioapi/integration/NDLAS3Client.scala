/*
 * Part of NDLA audio-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.integration

import no.ndla.common.aws.NdlaS3Client

class NDLAS3Client(bucket: String, region: Option[String]) extends NdlaS3Client(bucket, region)
