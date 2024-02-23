/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, Tag}
import no.ndla.common.model.{api => commonApi}
import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.api.ImageMetaInformationV2
import no.ndla.imageapi.model.domain._
import no.ndla.mapping

import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

trait TestData {

  class TestData {

    def updated(): NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)

    val ByNcSa: String = mapping.License.CC_BY_NC_SA.toString

    val elg = new ImageMetaInformation(
      id = Some(1),
      titles = List(ImageTitle("Elg i busk", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        ImageFileData(
          id = 123,
          fileName = "Elg.jpg",
          size = 2865539,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          1
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(Author("Redaksjonelt", "Kåre Knegg")),
        List(Author("Leverandør", "Leverans Leveransensen")),
        None,
        None,
        false
      ),
      tags = List(Tag(List("rovdyr", "elg"), "nb")),
      captions = List(ImageCaption("Elg i busk", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val apiElg: ImageMetaInformationV2 = api.ImageMetaInformationV2(
      "1",
      "Elg.jpg",
      api.ImageTitle("Elg i busk", "nb"),
      api.ImageAltText("Elg i busk", "nb"),
      "Elg.jpg",
      2865539,
      "image/jpeg",
      commonApi.Copyright(
        commonApi.License(
          "by-nc-sa",
          Some("Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic"),
          Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")
        ),
        Some("http://www.scanpix.no"),
        List(commonApi.Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      api.ImageTag(List("rovdyr", "elg"), "nb"),
      api.ImageCaption("Elg i busk", "nb"),
      List("nb"),
      updated(),
      "ndla123",
      ModelReleasedStatus.YES.toString,
      None,
      None
    )

    val apiBjorn: ImageMetaInformationV2 = ImageMetaInformationV2(
      id = "2",
      metaUrl = "",
      title = api.ImageTitle("Bjørn i busk", "nb"),
      alttext = api.ImageAltText("Elg i busk", "nb"),
      imageUrl = "",
      size = 141134,
      contentType = "image/jpeg",
      copyright = commonApi.Copyright(
        commonApi.License(
          "by-nc-sa",
          Some("Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic"),
          Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")
        ),
        Some("http://www.scanpix.no"),
        List(commonApi.Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = api.ImageTag(List("rovdyr", "bjørn"), "nb"),
      caption = api.ImageCaption("Bjørn i busk", "nb"),
      supportedLanguages = Seq("nb"),
      created = updated(),
      createdBy = "ndla124",
      modelRelease = ModelReleasedStatus.YES.toString,
      editorNotes = None,
      imageDimensions = None
    )

    val bjorn = new ImageMetaInformation(
      id = Some(2),
      titles = List(ImageTitle("Bjørn i busk", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 333,
          fileName = "Bjørn.jpg",
          size = 14113,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          2
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = List(Tag(List("rovdyr", "bjørn"), "nb")),
      captions = List(ImageCaption("Bjørn i busk", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val jerv: ImageMetaInformation = new ImageMetaInformation(
      id = Some(3),
      titles = List(ImageTitle("Jerv på stein", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 444,
          fileName = "Jerv.jpg",
          size = 39061,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          3
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = List(Tag(List("rovdyr", "jerv"), "nb")),
      captions = List(ImageCaption("Jerv på stein", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val mink = new ImageMetaInformation(
      id = Some(4),
      titles = List(ImageTitle("Overrasket mink", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 555,
          fileName = "Mink.jpg",
          size = 102559,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          4
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = List(Tag(List("rovdyr", "mink"), "nb")),
      captions = List(ImageCaption("Overrasket mink", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val rein = new ImageMetaInformation(
      id = Some(5),
      titles = List(ImageTitle("Rein har fanget rødtopp", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 667,
          fileName = "Rein.jpg",
          size = 504911,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          5
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = List(Tag(List("rovdyr", "rein", "jakt"), "nb")),
      captions = List(ImageCaption("Rein har fanget rødtopp", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val nonexisting: ImageMetaInformation = new ImageMetaInformation(
      id = Some(6),
      titles = List(ImageTitle("Krokodille på krok", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 777,
          fileName = "Krokodille.jpg",
          size = 2865539,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          6
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = List(Tag(List("rovdyr", "krokodille"), "nb")),
      captions = List(ImageCaption("Krokodille på krok", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val nonexistingWithoutThumb = new ImageMetaInformation(
      id = Some(6),
      titles = List(ImageTitle("Bison på sletten", "nb")),
      alttexts = List(ImageAltText("Elg i busk", "nb")),
      images = Seq(
        new ImageFileData(
          id = 888,
          fileName = "Bison.jpg",
          size = 2865539,
          contentType = "image/jpeg",
          dimensions = None,
          language = "nb",
          6
        )
      ),
      copyright = Copyright(
        ByNcSa,
        Some("http://www.scanpix.no"),
        List(Author("Fotograf", "Test Testesen")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = List(Tag(List("bison"), "nb")),
      captions = List(ImageCaption("Bison på sletten", "nb")),
      updatedBy = "ndla124",
      updated = updated(),
      created = updated(),
      createdBy = "ndla124",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    val testdata: List[ImageMetaInformation] = List(elg, bjorn, jerv, mink, rein)

    case class DiskImage(filename: String) extends ImageStream {
      override def contentType: String = s"image/$format"

      override def stream: InputStream = getClass.getResourceAsStream(s"/$filename")
      override def fileName: String    = filename

      override lazy val sourceImage: BufferedImage = ImageIO.read(stream)
      lazy val rawBytes: String                    = scala.io.Source.fromInputStream(stream).mkString
    }

    val NdlaLogoImage: DiskImage    = DiskImage("ndla_logo.jpg")
    val NdlaLogoGIFImage: DiskImage = DiskImage("ndla_logo.gif")
    val CCLogoSvgImage: DiskImage   = DiskImage("cc.svg")

    val ChildrensImage: DiskImage = DiskImage(
      "children-drawing-582306_640.jpg"
    ) // From https://pixabay.com/en/children-drawing-home-tree-meadow-582306/

    val searchSettings: SearchSettings = SearchSettings(
      query = None,
      minimumSize = None,
      language = "*",
      fallback = false,
      license = None,
      sort = Sort.ByIdAsc,
      page = None,
      pageSize = None,
      podcastFriendly = None,
      includeCopyrighted = false,
      shouldScroll = false,
      modelReleased = Seq.empty
    )
  }
}
