/*
 * Part of NDLA image-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migrationwithdependencies

import com.amazonaws.services.s3.model.{S3Object, S3ObjectInputStream}
import imageapi.db.migrationwithdependencies.V12__AddSizeMetaData
import no.ndla.imageapi.TestData.{CCLogoSvgImage, NdlaLogoImage}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.apache.http.client.methods.HttpRequestBase

import java.awt.image.BufferedImage
import scala.util.{Failure, Success}

class V12__AddSizeMetaDataTest extends UnitSuite with TestEnvironment {
  val migration = spy(new V12__AddSizeMetaData(props))

  val testUrl           = "http://test.test/1"
  val imageStreamMock   = mock[ImageStream]
  val bufferedImageMock = mock[BufferedImage]

  test("migration not do anything if the document already has new status format") {
    when(migration.get(testUrl)).thenReturn(Success(Some((100, 200))))
    val original =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"],"imageDimensions":{"width":100,"height":200}}"""

    migration.convertImageUpdate(original) should equal(original)
  }

  test("migration sets width and height to 0 if image does not exist in s3") {

    doReturn(Success(imageStreamMock)).when(imageStorage).get(any[String])
    when(migration.get(testUrl)).thenReturn(Failure(new Exception("no such key")))

    val old =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expected =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"],"imageDimensions":{"width":0,"height":0}}"""
    migration.convertImageUpdate(old) should equal(expected)
  }

  test("That svg's doesn't get a size") {
    val s3Mock      = mock[S3Object]
    val requestMock = mock[HttpRequestBase]

    val svgImage = new S3ObjectInputStream(CCLogoSvgImage.stream, requestMock)

    when(s3Mock.getObjectContent).thenReturn(svgImage)
    when(migration.getS3Object("some.svg")).thenReturn(Success(s3Mock))

    val old =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"some.svg","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""

    migration.convertImageUpdate(old) should be(old)
  }

  test("That real image gets its size") {
    val s3Mock      = mock[S3Object]
    val requestMock = mock[HttpRequestBase]
    val image       = new S3ObjectInputStream(NdlaLogoImage.stream, requestMock)
    when(s3Mock.getObjectContent).thenReturn(image)
    when(migration.getS3Object("some.jpg")).thenReturn(Success(s3Mock))

    val old =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"some.jpg","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expected =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"some.jpg","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"],"imageDimensions":{"width":189,"height":60}}"""

    migration.convertImageUpdate(old) should be(expected)
  }
}
