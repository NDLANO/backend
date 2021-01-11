package no.ndla.audioapi.model.domain

/** Metadata fields for [[AudioType.Podcast]] type audios */
case class PodcastMeta(
    header: Seq[Header],
    introduction: Seq[Introduction],
    coverPhoto: Seq[CoverPhoto],
    manuscript: Seq[Manuscript]
)

case class CoverPhoto(imageId: String, altText: String, language: String) extends LanguageField[(String, String)] {
  override def value: (String, String) = imageId -> altText
}

case class Manuscript(manuscript: String, language: String) extends LanguageField[String] {
  override def value: String = manuscript
}

case class Header(header: String, language: String) extends LanguageField[String] {
  override def value: String = header
}

case class Introduction(introduction: String, language: String) extends LanguageField[String] {
  override def value: String = introduction
}
