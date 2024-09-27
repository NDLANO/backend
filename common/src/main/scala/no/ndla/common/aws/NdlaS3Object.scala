/*
 * Part of NDLA backend.common.main
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.aws

import java.io.InputStream

case class NdlaS3Object(
    bucket: String,
    key: String,
    stream: InputStream,
    contentType: String,
    contentLength: Long
)
