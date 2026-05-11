/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.Clock
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import sttp.client4.PartialRequest
import sttp.client4.quick.*

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import scala.util.{Failure, Success}

class RawControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val imageName    = "ndla_logo.jpg"
  val imageGifName = "ndla_logo.gif"
  val imageSvgName = "logo.svg"

  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  val controller: RawController                              = new RawController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  val id    = 1L
  val idGif = 1L

  def req: PartialRequest[Array[Byte]] = basicRequest.response(asByteArrayAlways)

  override def beforeEach(): Unit = {
    reset(clock)
    when(imageRepository.withId(id)).thenReturn(Success(Some(TestData.bjorn)))
    when(readService.getImageFileName(id, None)).thenReturn(Success(Some(TestData.bjorn.images.head.fileName)))
    when(clock.now()).thenCallRealMethod()
  }

  test("That GET /image.jpg returns 200 if image was found") {
    when(imageStorage.getRaw(any[String])).thenReturn(Success(TestData.ndlaLogoImageS3Object))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName").send()
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /image.jpg returns 404 if image was not found") {
    when(imageStorage.getRaw(any[String])).thenReturn(Failure(new ImageNotFoundException("Image not found")))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName").send()
    res.code.code should be(404)
  }

  test("That GET /image.jpg with width resizing returns a resized image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?width=100").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(100)
  }

  test("That GET /image.jpg with height resizing returns a resized image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?height=40").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getHeight should equal(40)
  }

  test("That GET /image.jpg with an invalid value for width returns 400") {
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?width=twohundredandone").send()
    res.code.code should be(400)
  }

  test("That GET /image.jpg with cropping returns a cropped image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req
      .get(uri"http://localhost:$serverPort/image-api/raw/$imageName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50")
      .send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(94)
    image.getHeight should equal(30)
  }

  test("That GET /image.jpg with cropping and resizing returns a cropped and resized image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req
      .get(
        uri"http://localhost:$serverPort/image-api/raw/$imageName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
      .send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(50)
    image.getHeight should equal(15)
  }

  test("GET /id/1 returns 200 if the image was found") {
    when(imageStorage.getRaw(any[String])).thenReturn(Success(TestData.ndlaLogoImageS3Object))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 returns 404 if image was not found") {
    when(imageStorage.getRaw(any[String])).thenReturn(Failure(new ImageNotFoundException("Image not found")))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id").send()
    res.code.code should be(404)
  }

  test("That GET /id/1 with width resizing returns a resized image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?width=100").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(100)
  }

  test("That GET /id/1 with height resizing returns a resized image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?height=40").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getHeight should equal(40)
  }

  test("That GET /id/1 with an invalid value for width returns 400") {
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?width=twohundredandone").send()
    res.code.code should be(400)
  }

  test("That GET /id/1 with cropping returns a cropped image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req
      .get(uri"http://localhost:$serverPort/image-api/raw/id/$id?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50")
      .send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(94)
    image.getHeight should equal(30)
  }

  test("That GET /id/1 with cropping and resizing returns a cropped and resized image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoImageStream))
    val res = req
      .get(
        uri"http://localhost:$serverPort/image-api/raw/id/$id?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
      .send()
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(50)
    image.getHeight should equal(15)
  }

  test("That GET /imageGif.gif with width resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageGifName?width=100").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.gif with height resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageGifName?height=40").send()
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.gif with cropping returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req
      .get(
        uri"http://localhost:$serverPort/image-api/raw/$imageGifName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50"
      )
      .send()
    res.code.code should equal(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.jpg with cropping and resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req
      .get(
        uri"http://localhost:$serverPort/image-api/raw/$imageGifName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
      .send()
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /logo.svg with cropping and resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ccLogoSvgImageStream))
    val res = req
      .get(
        uri"http://localhost:$serverPort/image-api/raw/$imageSvgName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
      .send()
    res.code.code should equal(200)
    res.body should equal(TestData.ccLogoSvgImageStream.stream.readAllBytes())
  }

  test("That GET /id/1 for GIF image with width resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$idGif?width=100").send()
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 for GIF image with height resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$idGif?height=40").send()
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/2 for GIF image with cropping returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req
      .get(uri"http://localhost:$serverPort/image-api/raw/id/$idGif?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50")
      .send()

    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 for GIF image with cropping and resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(TestData.ndlaLogoGifImageStream))
    val res = req
      .get(
        uri"http://localhost:$serverPort/image-api/raw/id/$idGif?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
      .send()
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("that image is found by filename with non-ASCII characters") {
    val fileNameWithNonAsciiChars = "file æøå.svg"
    when(imageStorage.getRaw(eqTo(fileNameWithNonAsciiChars))).thenReturn(Success(TestData.ccLogoSvgImageS3Object))
    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$fileNameWithNonAsciiChars").send()
    res.code.code should equal(200)
    res.body should equal(TestData.ccLogoSvgImageStream.stream.readAllBytes())
  }

  test("That GET /image.jpg | /id/1 sets Cache-Control headers") {
    when(imageStorage.getRaw(any[String])).thenAnswer(_ => Success(TestData.ndlaLogoImageS3Object))
    val res1 = req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName").send()
    val res2 = req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id").send()

    res1.code.code should be(200)
    res2.code.code should be(200)
    res1.headers.find(_.is("cache-control")).get.value should be(props.RawControllerCacheControl)
    res2.headers.find(_.is("cache-control")).get.value should be(props.RawControllerCacheControl)
  }

  test("that GET /image/medium.webp returns 200 if image variant was found") {
    val fileName    = "image"
    val variantName = "medium.webp"
    when(imageStorage.getRaw(eqTo(s"$fileName/$variantName"))).thenReturn(Success(TestData.ndlaLogoImageS3Object))

    val res = req.get(uri"http://localhost:$serverPort/image-api/raw/$fileName/$variantName").send()

    res.code.code should be(200)
    res.body should be(TestData.ndlaLogoImageS3Object.stream.readAllBytes())
  }

  test("that GET /image.jpeg?download | /id/1?download sets Content-Disposition and returns raw unprocessed image") {
    when(imageStorage.getRaw(any[String])).thenAnswer(_ => Success(TestData.ndlaLogoImageS3Object))
    val res1 = req.get(uri"http://localhost:$serverPort/image-api/raw/image.jpg?download").send()
    val res2 = req.get(uri"http://localhost:$serverPort/image-api/raw/id/1?download").send()

    res1.code.code should be(200)
    res2.code.code should be(200)
    res1.headers.find(_.is("content-disposition")).get.value should be("attachment")
    res2.headers.find(_.is("content-disposition")).get.value should be("attachment")
    res1.body should be(TestData.ndlaLogoImageS3Object.stream.readAllBytes())
    res2.body should be(TestData.ndlaLogoImageS3Object.stream.readAllBytes())
  }

  test("that GET /image.jpeg?width=100&download | /id/1?width=100&download sets Content-Disposition") {
    when(imageStorage.get(any[String])).thenAnswer(_ => Success(TestData.ndlaLogoImageStream))
    val res1 = req.get(uri"http://localhost:$serverPort/image-api/raw/image.jpg?width=100&download").send()
    val res2 = req.get(uri"http://localhost:$serverPort/image-api/raw/id/1?width=100&download").send()

    res1.code.code should be(200)
    res2.code.code should be(200)
    res1.headers.find(_.is("content-disposition")).get.value should be("attachment")
    res2.headers.find(_.is("content-disposition")).get.value should be("attachment")
  }
}
