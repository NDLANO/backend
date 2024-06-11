/*
 * Part of NDLA image-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.repository

import java.net.Socket

import no.ndla.imageapi.model.domain.ImageTitle
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import scalikejdbc.DB

import scala.util.{Success, Try}
import scalikejdbc._

class ImageRepositoryTest extends IntegrationSuite(EnablePostgresContainer = true) with UnitSuite with TestEnvironment {
  override val dataSource         = testDataSource.get
  override val migrator           = new DBMigrator
  var repository: ImageRepository = _

  this.setDatabaseEnvironment()

  def serverIsListening: Boolean = {
    val server = props.MetaServer
    val port   = props.MetaPort
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  def emptyTestDatabase: Boolean =
    DB autoCommit (implicit session => {
      sql"delete from imagemetadata;".execute()(session)
    })

  override def beforeAll(): Unit = {
    super.beforeAll()
    DataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  override def beforeEach(): Unit = {
    repository = new ImageRepository
    emptyTestDatabase
  }

  test("That inserting and retrieving images works as expected") {
    postgresContainer.map(x => println(x.getJdbcUrl))
    val image1 = TestData.bjorn.copy(id = None, images = None, titles = Seq(ImageTitle("KyllingFisk", "nb")))

    val inserted1     = repository.insert(image1)
    val imageFile1    = TestData.bjorn.images.get.head
    val insertedFile1 = repository.insertImageFile(inserted1.id.get, imageFile1.fileName, imageFile1.toDocument())
    val expected1     = inserted1.copy(images = Some(Seq(insertedFile1.get)))

    val image2    = TestData.bjorn.copy(id = None, images = Some(List()), titles = Seq(ImageTitle("Apekatter", "nb")))
    val inserted2 = repository.insert(image2)

    val image3    = TestData.bjorn.copy(id = None, images = Some(List()), titles = Seq(ImageTitle("Ruslebiff", "nb")))
    val inserted3 = repository.insert(image3)

    repository.withId(inserted1.id.get).get should be(expected1)
    repository.withId(inserted2.id.get).get should be(inserted2)
    repository.withId(inserted3.id.get).get should be(inserted3)
  }

  test("That fetching images based on path works") {
    val path1 = "/some-path1.jpg"
    val path2 = "/some-path123.png"
    val path3 = "/some-path555.png"

    val image = TestData.bjorn.images.get.head

    val imageMeta1 = TestData.bjorn.copy(images = None)
    val meta1      = repository.insert(imageMeta1)
    val meta2      = repository.insert(imageMeta1)
    val meta3      = repository.insert(imageMeta1)

    val image1 = repository.insertImageFile(meta1.id.get, path1, image.copy(fileName = path1).toDocument()).get
    val image2 = repository.insertImageFile(meta2.id.get, path2, image.copy(fileName = path2).toDocument()).get
    val image3 = repository.insertImageFile(meta3.id.get, path3, image.copy(fileName = path3).toDocument()).get

    repository.getImageFromFilePath(path1).get should be(meta1.copy(images = Some(Seq(image1))))
    repository.getImageFromFilePath(path2).get should be(meta2.copy(images = Some(Seq(image2))))
    repository.getImageFromFilePath(path3).get should be(meta3.copy(images = Some(Seq(image3))))
    repository.getImageFromFilePath("/nonexistant.png") should be(None)
  }

  test("that fetching based on path works with and without slash") {
    val path1         = "/slash-path1.jpg"
    val imageFile1    = TestData.bjorn.images.get.head.copy(fileName = path1)
    val image1        = TestData.bjorn.copy(id = None, images = Some(Seq(imageFile1)))
    val inserted1     = repository.insert(image1)
    val insertedFile1 = repository.insertImageFile(inserted1.id.get, path1, imageFile1.toDocument()).get
    val expected1     = inserted1.copy(images = Some(Seq(insertedFile1)))

    val path2         = "no-slash-path2.jpg"
    val imageFile2    = TestData.bjorn.images.get.head.copy(fileName = path2)
    val image2        = TestData.bjorn.copy(id = None, images = Some(Seq(imageFile2)))
    val inserted2     = repository.insert(image2)
    val insertedFile2 = repository.insertImageFile(inserted2.id.get, path2, imageFile2.toDocument()).get
    val expected2     = inserted2.copy(images = Some(Seq(insertedFile2)))

    repository.getImageFromFilePath(path1).get should be(expected1)
    repository.getImageFromFilePath("/" + path1).get should be(expected1)

    repository.getImageFromFilePath(path2).get should be(expected2)
    repository.getImageFromFilePath("/" + path2).get should be(expected2)
  }

  test("That fetching image from url where there exists multiple works") {
    val path1        = "/fetch-path1.jpg"
    val imageFile1   = TestData.bjorn.images.get.head.copy(fileName = path1)
    val image1       = TestData.bjorn.copy(id = None, images = Some(Seq(imageFile1)))
    val inserted     = repository.insert(image1)
    val insertedFile = repository.insertImageFile(inserted.id.get, path1, imageFile1.toDocument()).get

    val expected = inserted.copy(images = Some(Seq(insertedFile)))

    repository.getImageFromFilePath(path1).get should be(expected)
  }

  test("That fetching image from url with special characters are escaped") {
    val path1        = "/path1.jpg"
    val imageFile1   = TestData.bjorn.images.get.head.copy(fileName = path1)
    val image1       = TestData.bjorn.copy(id = None, images = Some(Seq(imageFile1)))
    val inserted1    = repository.insert(image1)
    val insertedImg1 = repository.insertImageFile(inserted1.id.get, path1, imageFile1.toDocument()).get
    val expected1    = inserted1.copy(images = Some(Seq(insertedImg1)))

    val path2        = "/pa%h1.jpg"
    val imageFile2   = TestData.bjorn.images.get.head.copy(fileName = path2)
    val image2       = TestData.bjorn.copy(id = None, images = Some(Seq(imageFile2)))
    val inserted2    = repository.insert(image2)
    val insertedImg2 = repository.insertImageFile(inserted2.id.get, path2, imageFile2.toDocument()).get
    val expected2    = inserted2.copy(images = Some(Seq(insertedImg2)))

    repository.getImageFromFilePath(path1).get should be(expected1)
    repository.getImageFromFilePath(path2).get should be(expected2)
  }

}
