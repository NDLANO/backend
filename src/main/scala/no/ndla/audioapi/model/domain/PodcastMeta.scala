package no.ndla.audioapi.model.domain

/** Metadata fields for [[AudioType.Podcast]] type audios */
case class PodcastMeta(
    introduction: String,
    coverPhoto: CoverPhoto,
    language: String
) extends WithLanguage
