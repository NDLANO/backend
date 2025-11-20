/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.imageapi.db.migration.V23__MergeImageFileData.ImageFileRow
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V23__MergeImageFileDataTest extends UnitSuite, TestEnvironment {
  test("that `images` is replaced with the merged image file data") {
    val migration = new V23__MergeImageFileData

    val oldImageMeta = """
        |{
        |  "id": 1,
        |  "titles": [{"title": "Sample Image", "language": "en"}],
        |  "tags": [{"tags": ["sample", "image"], "language": "en"}],
        |  "images": [
        |    {
        |      "size": 12345,
        |      "language": "en",
        |      "variants": [],
        |      "contentType": "image/svg+xml"
        |    },
        |    {
        |      "size": 12345,
        |      "language": "nb",
        |      "variants": [],
        |      "contentType": "image/svg+xml"
        |    }
        |  ]
        |}
        |""".stripMargin

    val imageFile1 = """
        |{
        |  "size": 2048,
        |  "language": "en",
        |  "variants": [],
        |  "contentType": "image/svg+xml"
        |}
        |""".stripMargin

    val imageFile2 = """
        |{
        |  "size": 4096,
        |  "language": "nb",
        |  "variants": [],
        |  "contentType": "image/svg+xml"
        |}
        |""".stripMargin

    val expectedImageMetadata = """
        |{
        |  "id": 1,
        |  "titles": [{"title": "Sample Image", "language": "en"}],
        |  "tags": [{"tags": ["sample", "image"], "language": "en"}],
        |  "images": [
        |    {
        |      "size": 2048,
        |      "language": "en",
        |      "variants": [],
        |      "contentType": "image/svg+xml",
        |      "fileName": "image1.svg"
        |    },
        |    {
        |      "size": 4096,
        |      "language": "nb",
        |      "variants": [],
        |      "contentType": "image/svg+xml",
        |      "fileName": "image2.svg"
        |    }
        |  ]
        |}
        |""".stripMargin
    val expectedImageMetadataJson = CirceUtil.tryParse(expectedImageMetadata).get

    val imageFileRows = Seq(ImageFileRow(1, "image1.svg", imageFile1), ImageFileRow(2, "image2.svg", imageFile2))

    val convertedImageMetadata     = migration.convertImageMetadata(oldImageMeta, imageFileRows)
    val convertedImageMetadataJson = CirceUtil.tryParse(convertedImageMetadata).get

    convertedImageMetadataJson should be(expectedImageMetadataJson)
  }
}
