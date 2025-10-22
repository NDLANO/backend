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
import no.ndla.imageapi.service.ImageConverter
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import sttp.client3.quick.*

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import scala.util.{Failure, Success}
import sttp.client3.{Empty, RequestT}

class RawControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  import TestData.{CCLogoSvgImage, NdlaLogoGIFImage, NdlaLogoImage}
  val imageName    = "ndla_logo.jpg"
  val imageGifName = "ndla_logo.gif"
  val imageSvgName = "logo.svg"

  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override implicit lazy val imageConverter: ImageConverter  = new ImageConverter
  val controller: RawController                              = new RawController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  val id    = 1L
  val idGif = 1L

  def req: RequestT[Empty, Array[Byte], Any & Any] = basicRequest.response(asByteArrayAlways)

  override def beforeEach(): Unit = {
    reset(clock)
    when(imageRepository.withId(id)).thenReturn(Some(TestData.bjorn))
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoImage))
    when(readService.getImageFileName(id, None)).thenReturn(Success(Some(TestData.bjorn.images.get.head.fileName)))
    when(clock.now()).thenCallRealMethod()
  }

  test("That GET /image.jpg returns 200 if image was found") {
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName"))
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /image.jpg returns 404 if image was not found") {
    when(imageStorage.get(any[String])).thenReturn(Failure(new ImageNotFoundException("Image not found")))
    val res = simpleHttpClient.send[Array[Byte]](req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName"))
    res.code.code should be(404)
  }

  test("That GET /image.jpg with width resizing returns a resized image") {
    val res =
      simpleHttpClient.send[Array[Byte]](req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?width=100"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(100)
  }

  test("That GET /image.jpg with height resizing returns a resized image") {
    val res =
      simpleHttpClient.send[Array[Byte]](req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?height=40"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getHeight should equal(40)
  }

  test("That GET /image.jpg with an invalid value for width returns 400") {
    val res =
      simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?width=twohundredandone"))
    res.code.code should be(400)
  }

  test("That GET /image.jpg with cropping returns a cropped image") {
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/$imageName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50"
      )
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(94)
    image.getHeight should equal(30)
  }

  test("That GET /image.jpg with cropping and resizing returns a cropped and resized image") {
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/$imageName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(50)
    image.getHeight should equal(16)
  }

  test("GET /id/1 returns 200 if the image was found") {
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 returns 404 if image was not found") {
    when(imageStorage.get(any[String])).thenReturn(Failure(new ImageNotFoundException("Image not found")))
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id"))
    res.code.code should be(404)
  }

  test("That GET /id/1 with width resizing returns a resized image") {
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?width=100"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(100)
  }

  test("That GET /id/1 with height resizing returns a resized image") {
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?height=40"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getHeight should equal(40)
  }

  test("That GET /id/1 with an invalid value for width returns 400") {
    val res =
      simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?width=twohundredandone"))
    res.code.code should be(400)
  }

  test("That GET /id/1 with cropping returns a cropped image") {
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(94)
    image.getHeight should equal(30)
  }

  test("That GET /id/1 with cropping and resizing returns a cropped and resized image") {
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/id/$id?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
    )
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(50)
    image.getHeight should equal(16)
  }

  test("That GET /imageGif.gif with width resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/$imageGifName?width=100"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.gif with height resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/$imageGifName?height=40"))
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.gif with cropping returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/$imageGifName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50"
      )
    )
    res.code.code should equal(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.jpg with cropping and resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/$imageGifName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
    )
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /logo.svg with cropping and resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(CCLogoSvgImage))
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/$imageSvgName?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
    )
    res.code.code should equal(200)
    res.body should equal(CCLogoSvgImage.stream.readAllBytes())
  }

  test("That GET /id/1 with width resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$idGif?width=100"))
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 with height resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(req.get(uri"http://localhost:$serverPort/image-api/raw/id/$idGif?height=40"))
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/2 with cropping returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/id/$idGif?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50"
      )
    )

    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 with cropping and resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/id/$idGif?cropStartX=0&cropStartY=0&cropEndX=50&cropEndY=50&width=50"
      )
    )
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }
}
