/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.model.{GetObjectRequest, S3Object}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._

class AudioStorageServiceTest extends UnitSuite with TestEnvironment {
  val storage = new AudioStorage

  test("That objectExists returns true if an object exists") {
    when(amazonClient.getObject(any[GetObjectRequest])).thenReturn(mock[S3Object])
    storage.objectExists("some/audio/file.mp3") should equal (true)
  }

  test("That objectExists returns false if an object does not exist") {
    when(amazonClient.getObject(any[GetObjectRequest])).thenThrow(mock[AmazonClientException])
    storage.objectExists("some/audio/file.mp3") should equal (false)
  }
}
