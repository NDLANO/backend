package no.ndla.audioapi.integration

import no.ndla.common.aws.NdlaS3Client

class TranscribeS3Client(bucket: String, region: Option[String]) extends NdlaS3Client(bucket, region)
