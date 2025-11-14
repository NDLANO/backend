/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.domain.ProcessableImageStream
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.scalactic.{Equality, TolerantNumerics}

class ImageConverterTest extends UnitSuite with TestEnvironment {
  import TestData.NdlaLogoImage
  val service                   = new ImageConverter
  val (imageWidth, imageHeight) = (1000, 1000)

  test("transformCoordinates returns a CoordOptions object with correctly transformed coordinates") {
    service.transformCoordinates(imageWidth, imageHeight, PercentPoint(10, 5), PercentPoint(1, 20)) should equal(
      (PixelPoint(10, 50), PixelPoint(100, 200))
    )
    service.transformCoordinates(imageWidth, imageHeight, PercentPoint(10, 20), PercentPoint(1, 5)) should equal(
      (PixelPoint(10, 50), PixelPoint(100, 200))
    )
    service.transformCoordinates(imageWidth, imageHeight, PercentPoint(1, 5), PercentPoint(10, 20)) should equal(
      (PixelPoint(10, 50), PixelPoint(100, 200))
    )
    service.transformCoordinates(imageWidth, imageHeight, PercentPoint(1, 20), PercentPoint(10, 5)) should equal(
      (PixelPoint(10, 50), PixelPoint(100, 200))
    )
  }

  test("getWidthHeight returns the width and height of a segment to crop") {
    service.getWidthHeight(PixelPoint(10, 200), PixelPoint(100, 50), imageWidth, imageHeight) should equal((90, 150))
  }

  test("getWidthHeight returns max values if one coordinate is outside of image") {
    service.getWidthHeight(
      PixelPoint(10, 200),
      PixelPoint(imageWidth + 1, imageHeight + 1),
      imageWidth,
      imageHeight,
    ) should equal((990, 800))
  }

  test("s3ObjectToImageStream returns ProcessableImageStream for JPEG images") {
    val imageStream = service.s3ObjectToImageStream(TestData.ndlaLogoImageS3Object).failIfFailure
    imageStream shouldBe a[ProcessableImageStream]
  }

  test("crop crops an image according to given settings") {
    val croppedImage = service.crop(NdlaLogoImage, PercentPoint(0, 0), PercentPoint(50, 50)).failIfFailure
    croppedImage.image.width should equal(94)
    croppedImage.image.height should equal(30)
  }

  test("resize resizes image height correctly") {
    val resizedImage = service.resizeHeight(NdlaLogoImage, 30).failIfFailure
    resizedImage.image.height should equal(30)
  }

  test("resize resizes image width correctly") {
    val resizedImage = service.resizeWidth(NdlaLogoImage, 30).failIfFailure
    resizedImage.image.width should equal(30)
  }

  test("resize not resizes image if height is to big") {
    val resizedImage = service.resizeHeight(NdlaLogoImage, 400).failIfFailure
    resizedImage.image.height should equal(60)
  }

  test("resize not resizes image if width is to big") {
    val resizedImage = service.resizeWidth(NdlaLogoImage, 400).failIfFailure
    resizedImage.image.width should equal(189)
  }

  test("resize resizes an image according to image orientation if both height and width is specified") {
    val croppedImage = service.resize(NdlaLogoImage, 100, 60).failIfFailure
    croppedImage.image.width should equal(100)
    croppedImage.image.height should not equal 60
  }

  test("dynamic cropping should work as expected") {
    val croppedImage = service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), Some(10), Some(30), None).failIfFailure
    croppedImage.image.width should equal(10)
    croppedImage.image.height should equal(30)
  }

  test("dynamic cropping should scale according to original image size if only one dimension size is specified") {
    val image = service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), Some(100), None, None).failIfFailure
    image.image.width should equal(100)
    image.image.height should equal(31)

    val image2 = service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), None, Some(50), None).failIfFailure
    image2.image.width should equal(157)
    image2.image.height should equal(50)
  }

  test("dynamic crop should not manipulate image if neither target width or target height is specified") {
    val image = service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), None, None, None).failIfFailure
    image.image.width should equal(NdlaLogoImage.image.width)
    image.image.height should equal(NdlaLogoImage.image.height)
  }

  test("minimalCropSizesToPreserveRatio calculates correct image sizes given ratio") {
    service.minimalCropSizesToPreserveRatio(640, 426, 0.81) should equal((345, 426))
    service.minimalCropSizesToPreserveRatio(851, 597, 1.5) should equal((850, 567))
    service.minimalCropSizesToPreserveRatio(851, 597, 1.2) should equal((716, 597))
  }

  test(
    "minimalCropSizesToPreserveRatio calculates image sizes with (about) correct aspect ratio for lots of ratios and image sizes"
  ) {
    def testRatio(ratio: Double, width: Int, height: Int) = {
      implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)
      val (newWidth, newHeight)                     = service.minimalCropSizesToPreserveRatio(width, height, ratio)
      val calculatedRatio                           = newWidth.toDouble / newHeight.toDouble
      calculatedRatio should equal(ratio)
    }
    for {
      ratio  <- Seq(0.1, 0.2, 0.81, 1, 1.1, 1.5, 2, 5, 10)
      width  <- LazyList.range(10, 1000, 10)
      height <- LazyList.range(10, 1000, 10)
    } yield testRatio(ratio, width, height)
  }

  test("dynamic cropping with ratios should return image with (about) correct aspect ratio") {
    testRatio(0.81, 57, 50, 345, 426)
    testRatio(0.81, 0, 0, 345, 426)
    testRatio(0.81, 10, 10, 345, 426)
    testRatio(0.81, 90, 90, 345, 426)
    testRatio(1.5, 50, 50, 639, 426)
    testRatio(1.2, 50, 50, 511, 426)

    def testRatio(ratio: Double, focalX: Double, focalY: Double, expectedWidth: Int, expectedHeight: Int): Unit = {
      implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.01)
      val croppedImage                              = service
        .dynamicCrop(TestData.ChildrensImage, PercentPoint(focalX, focalY), Some(100), Some(100), Some(ratio))
        .failIfFailure
      val image           = croppedImage.image
      val calculatedRatio = image.width.toDouble / image.height.toDouble
      image.width should equal(expectedWidth)
      image.height should equal(expectedHeight)
      calculatedRatio should equal(ratio)
    }
  }
}
