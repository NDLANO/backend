package no.ndla.audioapi.model.domain

/** Metadata fields for [[AudioType.Podcast]] type audios */
case class PodcastMeta(
    header: String,
    introduction: String,
    coverPhoto: CoverPhoto,
    language: String
) extends WithLanguage

case class CoverPhoto(imageId: String, altText: String)
