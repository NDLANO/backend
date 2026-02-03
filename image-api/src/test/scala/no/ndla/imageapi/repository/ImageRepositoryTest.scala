/*
 * Part of NDLA image-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.repository

import java.net.Socket
import no.ndla.imageapi.model.domain.ImageTitle
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import no.ndla.database.{DataSource, DBMigrator, DBUtility}

import scala.util.{Success, Try}
import scalikejdbc.*

class ImageRepositoryTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override implicit lazy val props: ImageApiProperties = new ImageApiProperties
  override implicit lazy val dataSource: DataSource    = testDataSource.get
  override implicit lazy val dbUtility: DBUtility      = new DBUtility
  override implicit lazy val migrator: DBMigrator      = new DBMigrator
  var repository: ImageRepository                      = scala.compiletime.uninitialized

  def serverIsListening: Boolean = {
    val server = props.MetaServer.unsafeGet
    val port   = props.MetaPort.unsafeGet
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  def emptyTestDatabase: Boolean = dbUtility.writeSession(implicit session => {
    sql"delete from imagemetadata;".execute()(using session)
  })

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  override def beforeEach(): Unit = {
    repository = new ImageRepository
    emptyTestDatabase
  }

  test("That inserting and retrieving images works as expected") {
    val imageFile1 = TestData.bjorn.images.head
    val image1     = TestData.bjorn.copy(id = None, images = Seq(imageFile1), titles = Seq(ImageTitle("KyllingFisk", "nb")))
    val inserted1  = repository.insert(image1).failIfFailure
    val expected1  = image1.copy(id = inserted1.id)

    val image2    = TestData.bjorn.copy(id = None, images = Seq.empty, titles = Seq(ImageTitle("Apekatter", "nb")))
    val inserted2 = repository.insert(image2).failIfFailure
    val expected2 = image2.copy(id = inserted2.id)

    val image3    = TestData.bjorn.copy(id = None, images = Seq.empty, titles = Seq(ImageTitle("Ruslebiff", "nb")))
    val inserted3 = repository.insert(image3).failIfFailure
    val expected3 = image3.copy(id = inserted3.id)

    repository.withId(inserted1.id.get).failIfFailure should be(Some(expected1))
    repository.withId(inserted2.id.get).failIfFailure should be(Some(expected2))
    repository.withId(inserted3.id.get).failIfFailure should be(Some(expected3))
  }

  test("That fetching images based on path works") {
    val path1 = "/some-path1.jpg"
    val path2 = "/some-path123.png"
    val path3 = "/some-path555.png"

    val image = TestData.bjorn.images.head

    val image1      = image.copy(fileName = path1)
    val meta1       = TestData.bjorn.copy(images = Seq(image1))
    val metaWithId1 = repository.insert(meta1).failIfFailure

    val image2      = image.copy(fileName = path2)
    val meta2       = TestData.bjorn.copy(images = Seq(image2))
    val metaWithId2 = repository.insert(meta2).failIfFailure

    val image3      = image.copy(fileName = path3)
    val meta3       = TestData.bjorn.copy(images = Seq(image3))
    val metaWithId3 = repository.insert(meta3).failIfFailure

    repository.getImageFromFilePath(path1).failIfFailure should be(Some(metaWithId1))
    repository.getImageFromFilePath(path2).failIfFailure should be(Some(metaWithId2))
    repository.getImageFromFilePath(path3).failIfFailure should be(Some(metaWithId3))
    repository.getImageFromFilePath("/nonexistent.png") should be(Success(None))
  }

  test("that fetching based on path works with and without slash") {
    val path1      = "/slash-path1.jpg"
    val imageFile1 = TestData.bjorn.images.head.copy(fileName = path1)
    val image1     = TestData.bjorn.copy(id = None, images = Seq(imageFile1))
    val inserted1  = repository.insert(image1).failIfFailure
    val expected1  = inserted1.copy(images = Seq(imageFile1))

    val path2      = "no-slash-path2.jpg"
    val imageFile2 = TestData.bjorn.images.head.copy(fileName = path2)
    val image2     = TestData.bjorn.copy(id = None, images = Seq(imageFile2))
    val inserted2  = repository.insert(image2).failIfFailure
    val expected2  = inserted2.copy(images = Seq(imageFile2))

    repository.getImageFromFilePath(path1).get should be(Some(expected1))
    repository.getImageFromFilePath("/" + path1).get should be(Some(expected1))

    repository.getImageFromFilePath(path2).get should be(Some(expected2))
    repository.getImageFromFilePath("/" + path2).get should be(Some(expected2))
  }

  test("That fetching image from url where there exists multiple works") {
    val path1      = "/fetch-path1.jpg"
    val imageFile1 = TestData.bjorn.images.head.copy(fileName = path1)
    val image1     = TestData.bjorn.copy(id = None, images = Seq(imageFile1))
    val inserted   = repository.insert(image1).failIfFailure

    val expected = inserted.copy(images = Seq(imageFile1))

    repository.getImageFromFilePath(path1).get should be(Some(expected))
  }

  test("That fetching image from url with special characters are escaped") {
    val path1      = "/path1.jpg"
    val imageFile1 = TestData.bjorn.images.head.copy(fileName = path1)
    val image1     = TestData.bjorn.copy(id = None, images = Seq(imageFile1))
    val inserted1  = repository.insert(image1).failIfFailure
    val expected1  = inserted1.copy(images = Seq(imageFile1))

    val path2      = "/pa%h1.jpg"
    val imageFile2 = TestData.bjorn.images.head.copy(fileName = path2)
    val image2     = TestData.bjorn.copy(id = None, images = Seq(imageFile2))
    val inserted2  = repository.insert(image2).failIfFailure
    val expected2  = inserted2.copy(images = Seq(imageFile2))

    repository.getImageFromFilePath(path1).get should be(Some(expected1))
    repository.getImageFromFilePath(path2).get should be(Some(expected2))
  }

}
