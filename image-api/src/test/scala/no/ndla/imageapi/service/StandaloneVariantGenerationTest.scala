/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.domain.{ImageContentType, ImageMetaInformation, ImageVariant, ImageVariantGenerationMode, ImageVariantSize, ModelReleasedStatus}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.article.Copyright
import org.mockito.ArgumentMatchers.{any, startsWith, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, reset, times, verify, when}
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class StandaloneVariantGenerationTest extends UnitSuite with TestEnvironment {
  override implicit lazy val writeService: WriteService = WriteService()
  val standaloneVariantGeneration                       =
    StandaloneVariantGeneration(writeService, imageStorage, imageRepository, dbUtility, props)

  private val originalVariant  = ImageVariant(ImageVariantSize.Icon, "existing/icon.webp")
  private val generatedVariant = ImageVariant(ImageVariantSize.Medium, "generated/medium.webp")

  override def beforeEach(): Unit = {
    reset(imageRepository, imageStorage)
    when(imageRepository.getImageMetaBatched(eqTo(20L))).thenReturn(Success(Iterator.empty))
    when(imageRepository.update(any[ImageMetaInformation], any[Long])(using any[DBSession])).thenReturn(Success(1))
    when(imageStorage.deleteObjects(any[Seq[String]])).thenReturn(Success(()))
  }

  test("missing_only should generate variants only for image files without variants") {
    val missing        = TestData.elgFileData.copy(language = "nb")
    val existing       = TestData.elgFileData.copy(language = "en", variants = Seq(originalVariant))
    val meta           = TestData.elg.copy(images = Seq(missing, existing))
    val updatedMissing = missing.copy(variants = Seq(generatedVariant))

    when(imageRepository.getImageMetaBatched(eqTo(20L))).thenReturn(Success(Iterator.single(Seq(meta))))

    standaloneVariantGeneration
      .generateVariantsForExistingImages(ImageVariantGenerationMode.MissingOnly, ignoreMissingObjects = false)
      .get

    verify(imageStorage, Mockito.only()).uploadFromStream(startsWith(s"${missing.getFileStem}/"), any, any, any)
    verify(imageRepository, times(1)).update(eqTo(meta.copy(images = Seq(updatedMissing, existing))), eqTo(1L))(using
      any[DBSession]
    )
    verify(imageStorage, never).deleteObjects(any[Seq[String]])
  }

  test("replace_all should regenerate all processable image files and delete obsolete variant keys") {
    val first         = TestData.elgFileData.copy(language = "nb", variants = Seq(originalVariant))
    val second        = TestData.elgFileData.copy(language = "en", variants = Seq(originalVariant))
    val meta          = TestData.elg.copy(images = Seq(first, second))
    val updatedFirst  = first.copy(variants = Seq(generatedVariant))
    val updatedSecond = second.copy(variants = Seq(generatedVariant.copy(bucketKey = "generated/en-medium.webp")))

    when(imageRepository.getImageMetaBatched(eqTo(20L))).thenReturn(Success(Iterator.single(Seq(meta))))

    standaloneVariantGeneration
      .generateVariantsForExistingImages(ImageVariantGenerationMode.ReplaceAll, ignoreMissingObjects = true)
      .get

    verify(writeService, times(1)).generateAndUploadVariantsForImageFileDataAsync(eqTo(true))(eqTo(meta), eqTo(first))(
      using any
    )
    verify(writeService, times(1)).generateAndUploadVariantsForImageFileDataAsync(eqTo(true))(eqTo(meta), eqTo(second))(
      using any
    )
    verify(imageRepository, times(1)).update(eqTo(meta.copy(images = Seq(updatedFirst, updatedSecond))), eqTo(1L))(using
      any[DBSession]
    )
    verify(imageStorage, times(1)).deleteObjects(eqTo(Seq("existing/icon.webp")))
  }

  test("replace_all should skip non-processable content types") {
    val gif  = imageFile("nb", contentType = ImageContentType.Gif, variants = Seq(originalVariant))
    val meta = imageMeta(Seq(gif))

    when(imageRepository.getImageMetaBatched(eqTo(20L))).thenReturn(Success(Iterator.single(Seq(meta))))

    standaloneVariantGeneration
      .generateVariantsForExistingImages(ImageVariantGenerationMode.ReplaceAll, ignoreMissingObjects = false)
      .get

    verify(writeService, never).generateAndUploadVariantsForImageFileDataAsync(eqTo(false))(eqTo(meta), eqTo(gif))(using
      any
    )
    verify(imageRepository, never).update(any[ImageMetaInformation], any[Long])(using any[DBSession])
  }

  test("replace_all should fail if obsolete variant cleanup fails") {
    val file    = imageFile("nb", variants = Seq(originalVariant))
    val meta    = imageMeta(Seq(file))

    when(imageRepository.getImageMetaBatched(eqTo(20L))).thenReturn(Success(Iterator.single(Seq(meta))))
    when(imageStorage.deleteObjects(eqTo(Seq("existing/icon.webp")))).thenReturn(
      Failure(new RuntimeException("cleanup failed"))
    )

    standaloneVariantGeneration
      .generateVariantsForExistingImages(ImageVariantGenerationMode.ReplaceAll, ignoreMissingObjects = false)
      .isFailure should be(true)
  }
}
