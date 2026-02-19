/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.common.aws.NdlaS3Object
import no.ndla.common.model.{NDLADate, api as commonApi}
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, ContributorType, Tag, UploadedFile}
import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.api.ImageMetaInformationV2DTO
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.service.ImageConverter
import no.ndla.mapping
import no.ndla.mapping.License

import java.io.{ByteArrayInputStream, File}

class TestData(using props: Props) {
  def updated(): NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)

  val ByNcSa: String = mapping.License.CC_BY_NC_SA.toString

  val elg = new ImageMetaInformation(
    id = Some(1),
    titles = List(ImageTitle("Elg i busk", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      ImageFileData(
        fileName = "Elg.jpg",
        size = 2865539,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(Author(ContributorType.Editorial, "Kåre Knegg")),
      List(Author(ContributorType.Supplier, "Leverans Leveransensen")),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("rovdyr", "elg"), "nb")),
    captions = List(ImageCaption("Elg i busk", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val apiElg: ImageMetaInformationV2DTO = api.ImageMetaInformationV2DTO(
    "1",
    "Elg.jpg",
    api.ImageTitleDTO("Elg i busk", "nb"),
    api.ImageAltTextDTO("Elg i busk", "nb"),
    "Elg.jpg",
    2865539,
    "image/jpeg",
    commonApi.CopyrightDTO(
      commonApi.LicenseDTO(
        License.CC_BY_NC_SA.toString,
        Some("Creative Commons Attribution-NonCommercial-ShareAlike 4.0 Generic"),
        Some("https://creativecommons.org/licenses/by-nc-sa/4.0/"),
      ),
      Some("http://www.scanpix.no"),
      List(commonApi.AuthorDTO(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    api.ImageTagDTO(List("rovdyr", "elg"), "nb"),
    api.ImageCaptionDTO("Elg i busk", "nb"),
    List("nb"),
    updated(),
    "ndla123",
    ModelReleasedStatus.YES.toString,
    None,
    None,
  )

  val apiBjorn: ImageMetaInformationV2DTO = ImageMetaInformationV2DTO(
    id = "2",
    metaUrl = "",
    title = api.ImageTitleDTO("Bjørn i busk", "nb"),
    alttext = api.ImageAltTextDTO("Elg i busk", "nb"),
    imageUrl = "",
    size = 141134,
    contentType = "image/jpeg",
    copyright = commonApi.CopyrightDTO(
      commonApi.LicenseDTO(
        License.CC_BY_NC_SA.toString,
        Some("Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic"),
        Some("https://creativecommons.org/licenses/by-nc-sa/2.0/"),
      ),
      Some("http://www.scanpix.no"),
      List(commonApi.AuthorDTO(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = api.ImageTagDTO(List("rovdyr", "bjørn"), "nb"),
    caption = api.ImageCaptionDTO("Bjørn i busk", "nb"),
    supportedLanguages = Seq("nb"),
    created = updated(),
    createdBy = "ndla124",
    modelRelease = ModelReleasedStatus.YES.toString,
    editorNotes = None,
    imageDimensions = None,
  )

  val bjorn = new ImageMetaInformation(
    id = Some(2),
    titles = List(ImageTitle("Bjørn i busk", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      new ImageFileData(
        fileName = "Bjørn.jpg",
        size = 14113,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("rovdyr", "bjørn"), "nb")),
    captions = List(ImageCaption("Bjørn i busk", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val jerv: ImageMetaInformation = new ImageMetaInformation(
    id = Some(3),
    titles = List(ImageTitle("Jerv på stein", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      new ImageFileData(
        fileName = "Jerv.jpg",
        size = 39061,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("rovdyr", "jerv"), "nb")),
    captions = List(ImageCaption("Jerv på stein", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val mink = new ImageMetaInformation(
    id = Some(4),
    titles = List(ImageTitle("Overrasket mink", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      new ImageFileData(
        fileName = "Mink.jpg",
        size = 102559,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("rovdyr", "mink"), "nb")),
    captions = List(ImageCaption("Overrasket mink", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val rein = new ImageMetaInformation(
    id = Some(5),
    titles = List(ImageTitle("Rein har fanget rødtopp", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      new ImageFileData(
        fileName = "Rein.jpg",
        size = 504911,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("rovdyr", "rein", "jakt"), "nb")),
    captions = List(ImageCaption("Rein har fanget rødtopp", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val nonexisting: ImageMetaInformation = new ImageMetaInformation(
    id = Some(6),
    titles = List(ImageTitle("Krokodille på krok", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      new ImageFileData(
        fileName = "Krokodille.jpg",
        size = 2865539,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("rovdyr", "krokodille"), "nb")),
    captions = List(ImageCaption("Krokodille på krok", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val nonexistingWithoutThumb = new ImageMetaInformation(
    id = Some(6),
    titles = List(ImageTitle("Bison på sletten", "nb")),
    alttexts = List(ImageAltText("Elg i busk", "nb")),
    images = Seq(
      new ImageFileData(
        fileName = "Bison.jpg",
        size = 2865539,
        contentType = "image/jpeg",
        dimensions = None,
        variants = Seq.empty,
        language = "nb",
      )
    ),
    copyright = Copyright(
      ByNcSa,
      Some("http://www.scanpix.no"),
      List(Author(ContributorType.Photographer, "Test Testesen")),
      List(),
      List(),
      None,
      None,
      false,
    ),
    tags = List(Tag(List("bison"), "nb")),
    captions = List(ImageCaption("Bison på sletten", "nb")),
    updatedBy = "ndla124",
    updated = updated(),
    created = updated(),
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty,
    inactive = false,
  )

  val testdata: List[ImageMetaInformation] = List(elg, bjorn, jerv, mink, rein)

  def mockS3ObjectFromDisk(fileName: String, contentType: String): NdlaS3Object = {
    val bytes  = getClass.getResourceAsStream(s"/$fileName").readAllBytes()
    val stream = new ByteArrayInputStream(bytes)
    NdlaS3Object("", fileName, stream, contentType, bytes.length)
  }

  private val imageConverter: ImageConverter = new ImageConverter

  def ndlaLogoImageS3Object: NdlaS3Object          = mockS3ObjectFromDisk("ndla_logo.jpg", "image/jpeg")
  def ndlaLogoImageStream: ImageStream.Processable = imageConverter
    .s3ObjectToImageStream(ndlaLogoImageS3Object)
    .get
    .asInstanceOf[ImageStream.Processable]
  val NdlaLogoImage: ProcessableImage = ProcessableImage.fromStream(ndlaLogoImageStream).get

  def ndlaLogoGIFImageS3Object: NdlaS3Object  = mockS3ObjectFromDisk("ndla_logo.gif", "image/gif")
  def ndlaLogoGifImageStream: ImageStream.Gif = imageConverter
    .s3ObjectToImageStream(ndlaLogoGIFImageS3Object)
    .get
    .asInstanceOf[ImageStream.Gif]

  def ccLogoSvgImageS3Object: NdlaS3Object            = mockS3ObjectFromDisk("cc.svg", "image/svg+xml")
  def ccLogoSvgImageStream: ImageStream.Unprocessable = imageConverter
    .s3ObjectToImageStream(ccLogoSvgImageS3Object)
    .get
    .asInstanceOf[ImageStream.Unprocessable]

  // From https://pixabay.com/en/children-drawing-home-tree-meadow-582306/
  private val childrensImageFileName = "children-drawing-582306_640.jpg"
  private val childrensImageS3Object = mockS3ObjectFromDisk(childrensImageFileName, "image/jpeg")
  private val childrensImageStream   = imageConverter
    .s3ObjectToImageStream(childrensImageS3Object)
    .get
    .asInstanceOf[ImageStream.Processable]
  val ChildrensImage: ProcessableImage = ProcessableImage.fromStream(childrensImageStream).get

  val childrensImageUploadedFile: UploadedFile = {
    val file = new File(getClass.getResource(s"/$childrensImageFileName").toURI)
    UploadedFile("file", Some(childrensImageFileName), file.length(), Some("image/jpeg"), file)
  }

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
    shouldScroll = false,
    modelReleased = Seq.empty,
    userFilter = List.empty,
    inactive = None,
    widthFrom = None,
    widthTo = None,
    heightFrom = None,
    heightTo = None,
  )
}
