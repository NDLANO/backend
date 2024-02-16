/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import org.mockito.Strictness

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import scala.util.{Failure, Success}
import sttp.client3.quick._

class RawControllerTest extends UnitSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  import TestData.{CCLogoSvgImage, NdlaLogoGIFImage, NdlaLogoImage}
  val imageName    = "ndla_logo.jpg"
  val imageGifName = "ndla_logo.gif"
  val imageSvgName = "logo.svg"

  override val imageConverter               = new ImageConverter
  val controller                            = new RawController
  override val services: List[Service[Eff]] = List(controller)

  val id    = 1L
  val idGif = 1L

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer("RawControllerTest", serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  def req = basicRequest.response(asByteArrayAlways)

  override def beforeEach(): Unit = {
    reset(clock)
    when(imageRepository.withId(id)).thenReturn(Some(TestData.bjorn))
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoImage))
    when(readService.getImageFileName(id, None)).thenReturn(Some(TestData.bjorn.images.head.fileName))
    when(clock.now()).thenCallRealMethod()
  }

  test("That GET /image.jpg returns 200 if image was found") {
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName")
    )
    res.code.code should be(200)

    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /image.jpg returns 404 if image was not found") {
    when(imageStorage.get(any[String]))
      .thenReturn(Failure(mock[ImageNotFoundException](withSettings.strictness(Strictness.Lenient))))
    val res = simpleHttpClient.send[Array[Byte]](
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName")
    )
    res.code.code should be(404)
  }

  test("That GET /image.jpg with width resizing returns a resized image") {
    val res = simpleHttpClient.send[Array[Byte]](
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?width=100")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(100)
  }

  test("That GET /image.jpg with height resizing returns a resized image") {
    val res = simpleHttpClient.send[Array[Byte]](
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?height=40")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getHeight should equal(40)
  }

  test("That GET /image.jpg with an invalid value for width returns 400") {
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageName?width=twohundredandone")
    )
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
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 returns 404 if image was not found") {
    when(imageStorage.get(any[String]))
      .thenReturn(Failure(mock[ImageNotFoundException](withSettings.strictness(Strictness.Lenient))))
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id")
    )
    res.code.code should be(404)
  }

  test("That GET /id/1 with width resizing returns a resized image") {
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?width=100")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(100)
  }

  test("That GET /id/1 with height resizing returns a resized image") {
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?height=40")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getHeight should equal(40)
  }

  test("That GET /id/1 with an invalid value for width returns 400") {
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/id/$id?width=twohundredandone")
    )
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
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageGifName?width=100")
    )
    res.code.code should be(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /imageGif.gif with height resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(
      req.get(uri"http://localhost:$serverPort/image-api/raw/$imageGifName?height=40")
    )
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
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/id/$idGif?width=100"
      )
    )
    res.code.code should equal(200)
    val image = ImageIO.read(new ByteArrayInputStream(res.body))
    image.getWidth should equal(189)
    image.getHeight should equal(60)
  }

  test("That GET /id/1 with height resizing returns the original image") {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoGIFImage))
    val res = simpleHttpClient.send(
      req.get(
        uri"http://localhost:$serverPort/image-api/raw/id/$idGif?height=40"
      )
    )
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

  test("That no endpoints are shadowed") {
    import sttp.tapir.testing.EndpointVerifier
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }
}
