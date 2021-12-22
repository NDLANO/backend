/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service
import com.amazonaws.AmazonServiceException
import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

class FileStorageServiceTest extends UnitSuite with TestEnvironment {
  override lazy val fileStorage = new FileStorageService

  override def beforeEach(): Unit = reset(amazonClient)

  test("That objectExists returns true when file exists") {
    val argumentCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    when(amazonClient.doesObjectExist(any[String], any[String])).thenReturn(true)

    fileStorage.resourceExists("existingKey") should be(true)

    verify(amazonClient).doesObjectExist(any[String], argumentCaptor.capture())
    argumentCaptor.getValue should be("resources/existingKey")
  }

  test("That objectExists returns false when file does not exist") {
    when(amazonClient.doesObjectExist(any[String], any[String])).thenThrow(mock[AmazonServiceException])
    val argumentCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

    fileStorage.resourceExists("nonExistingKey") should be(false)

    verify(amazonClient).doesObjectExist(any[String], argumentCaptor.capture())
    argumentCaptor.getValue should be("resources/nonExistingKey")
  }
}
