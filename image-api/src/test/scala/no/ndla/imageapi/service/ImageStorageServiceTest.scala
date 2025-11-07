/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.aws.NdlaS3Object
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

import scala.util.{Failure, Success}

class ImageStorageServiceTest extends UnitSuite with TestEnvironment {

  val ImageStorageName           = props.StorageName
  val ImageWithNoThumb           = TestData.nonexistingWithoutThumb
  val Content                    = "content"
  val ContentType                = "image/jpeg"
  override lazy val imageStorage = new ImageStorageService

  override def beforeEach(): Unit = {
    reset(s3Client)
  }

  test("That AmazonImageStorage.objectExists returns true when image exists") {
    when(s3Client.objectExists(any)).thenReturn(true)
    assert(imageStorage.objectExists("existingKey"))
  }

  test("That AmazonImageStorage.objectExists returns false when image does not exist") {
    when(s3Client.objectExists(any)).thenReturn(false)
    imageStorage.objectExists("nonExistingKey") should be(false)
  }

  test("That AmazonImageStorage.get returns a tuple with contenttype and data when the key exists") {
    val s3Object = NdlaS3Object("bucket", "existing", TestData.NdlaLogoImage.toStream, ContentType, 0)
    when(s3Client.getObject(any)).thenReturn(Success(s3Object))

    val image = imageStorage.get("existing").failIfFailure
    assert(image.contentType == ContentType)
    assert(image.fileName == "existing")
  }

  test("That AmazonImageStorage.get returns None when the key does not exist") {
    when(s3Client.getObject(any)).thenReturn(Failure(NoSuchKeyException.builder().build()))
    assert(imageStorage.get("nonexisting").isFailure)
  }

}
