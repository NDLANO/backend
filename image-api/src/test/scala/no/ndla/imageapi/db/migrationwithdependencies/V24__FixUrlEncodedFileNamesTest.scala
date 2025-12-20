/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migrationwithdependencies

import no.ndla.common.CirceUtil
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{reset, when}

class V24__FixUrlEncodedFileNamesTest extends UnitSuite with TestEnvironment {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(s3Client)
  }

  test("that fileName is decoded when encoded key is missing but decoded exists") {
    val migration    = new V24__FixUrlEncodedFileNames
    val oldImageMeta = """
        |{
        |  "images": [
        |    {
        |      "fileName": "foo%20bar.jpg",
        |      "size": 123,
        |      "language": "nb",
        |      "variants": [],
        |      "contentType": "image/jpeg"
        |    }
        |  ]
        |}
        |""".stripMargin
    val expected = """
        |{
        |  "images": [
        |    {
        |      "fileName": "foo bar.jpg",
        |      "size": 123,
        |      "language": "nb",
        |      "variants": [],
        |      "contentType": "image/jpeg"
        |    }
        |  ]
        |}
        |""".stripMargin
    when(s3Client.objectExists(eqTo("foo%20bar.jpg"))).thenReturn(false)
    when(s3Client.objectExists(eqTo("foo bar.jpg"))).thenReturn(true)

    val resultJson   = CirceUtil.unsafeParse(migration.convertColumn(oldImageMeta))
    val expectedJson = CirceUtil.unsafeParse(expected)
    resultJson should be(expectedJson)
  }

  test("that fileName is left as-is when encoded key exists") {
    val migration = new V24__FixUrlEncodedFileNames
    val imageMeta = """
        |{
        |  "images": [
        |    {
        |      "fileName": "foo%20bar.jpg",
        |      "size": 123,
        |      "language": "nb",
        |      "variants": [],
        |      "contentType": "image/jpeg"
        |    }
        |  ]
        |}
        |""".stripMargin
    when(s3Client.objectExists(eqTo("foo%20bar.jpg"))).thenReturn(true)

    val resultJson   = CirceUtil.unsafeParse(migration.convertColumn(imageMeta))
    val expectedJson = CirceUtil.unsafeParse(imageMeta)
    resultJson should be(expectedJson)
  }

  test("that fileName is left as-is when neither encoded nor decoded key exists") {
    val migration = new V24__FixUrlEncodedFileNames
    val imageMeta = """
                      |{
                      |  "images": [
                      |    {
                      |      "fileName": "foo%20bar.jpg",
                      |      "size": 123,
                      |      "language": "nb",
                      |      "variants": [],
                      |      "contentType": "image/jpeg"
                      |    }
                      |  ]
                      |}
                      |""".stripMargin
    when(s3Client.objectExists(eqTo("foo%20bar.jpg"))).thenReturn(false)
    when(s3Client.objectExists(eqTo("foo bar.jpg"))).thenReturn(false)

    val resultJson   = CirceUtil.unsafeParse(migration.convertColumn(imageMeta))
    val expectedJson = CirceUtil.unsafeParse(imageMeta)
    resultJson should be(expectedJson)
  }
}
