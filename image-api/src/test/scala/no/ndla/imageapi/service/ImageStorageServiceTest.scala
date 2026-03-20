/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.aws.NdlaS3Object
import no.ndla.imageapi.model.domain.{ImageContentType, ImageMetaInformation}
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import software.amazon.awssdk.services.s3.model.{CopyObjectResponse, NoSuchKeyException}

import scala.util.{Failure, Success}

class ImageStorageServiceTest extends UnitSuite with TestEnvironment {

  val ImageStorageName: String               = props.StorageName
  val ImageWithNoThumb: ImageMetaInformation = TestData.nonexistingWithoutThumb
  val Content                                = "content"
  val ContentType: ImageContentType          = ImageContentType.Jpeg
  override lazy val imageStorage             = new ImageStorageService

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
    val s3Object = NdlaS3Object("bucket", "existing", TestData.ndlaLogoImageStream.stream, ContentType.toString, 0)
    when(s3Client.getObject(any)).thenReturn(Success(s3Object))

    val image = imageStorage.get("existing").failIfFailure
    assert(image.contentType == ContentType)
    assert(image.fileName == "existing")
  }

  test("That AmazonImageStorage.get returns None when the key does not exist") {
    when(s3Client.getObject(any)).thenReturn(Failure(NoSuchKeyException.builder().build()))
    assert(imageStorage.get("nonexisting").isFailure)
  }

  test("That AmazonImageStorage.get fixes content-type when it is binary/octet-stream") {
    val s3Object = NdlaS3Object("bucket", "existing", TestData.ndlaLogoImageStream.stream, "binary/octet-stream", 100)
    when(s3Client.getObject(any)).thenReturn(Success(s3Object))
    when(readService.getImageFileFromFilePath(any)).thenReturn(Success(ImageWithNoThumb.images.head))
    when(s3Client.updateContentType(any, any)).thenReturn(Success(mock[CopyObjectResponse]))

    imageStorage.get("existing")

    verify(s3Client, times(1)).getObject(eqTo(s3Object.key))
    verify(s3Client, times(1)).updateContentType(eqTo(s3Object.key), eqTo("image/jpeg"))
  }
}
