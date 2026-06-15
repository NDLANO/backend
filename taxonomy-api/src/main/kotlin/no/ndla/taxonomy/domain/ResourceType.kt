/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import io.swagger.v3.oas.annotations.media.Schema
import java.net.URI
import no.ndla.taxonomy.config.Constants

private fun rtTranslations(en: String, nb: String, nn: String, se: String) =
    listOf(
        JsonTranslation(en, "en"),
        JsonTranslation(nb, "nb"),
        JsonTranslation(nn, "nn"),
        JsonTranslation(se, "se"),
    )

@Schema(enumAsRef = true)
enum class ResourceType(
    val publicId: URI,
    val translations: List<JsonTranslation>,
    val order: Int,
    val parent: ResourceType? = null,
) {
  LEARNING_PATH(
      publicId = URI.create("urn:resourcetype:learningPath"),
      translations =
          rtTranslations(
              en = "Learning Path",
              nb = "Læringssti",
              nn = "Læringssti",
              se = "Oahppanbálggis",
          ),
      order = 0,
  ),
  SUBJECT_MATERIAL(
      publicId = URI.create("urn:resourcetype:subjectMaterial"),
      translations =
          rtTranslations(
              en = "Subject Material",
              nb = "Fagstoff",
              nn = "Fagstoff",
              se = "Fágaávnnas",
          ),
      order = 1,
  ),
  TASKS_AND_ACTIVITIES(
      publicId = URI.create("urn:resourcetype:tasksAndActivities"),
      translations =
          rtTranslations(en = "Task", nb = "Oppgave", nn = "Oppgåve", se = "Bargobihtát"),
      order = 2,
  ),
  REVIEW_RESOURCE(
      publicId = URI.create("urn:resourcetype:reviewResource"),
      translations =
          rtTranslations(
              en = "Assessment Resources",
              nb = "Vurderingsressurs",
              nn = "Vurderingsressurs",
              se = "Árvvoštallanresursa",
          ),
      order = 3,
  ),
  SOURCE_MATERIAL(
      publicId = URI.create("urn:resourcetype:sourceMaterial"),
      translations =
          rtTranslations(
              en = "External resources",
              nb = "Kildemateriell",
              nn = "Kjeldemateriale",
              se = "Gáldomateriála",
          ),
      order = 4,
  ),
  CONCEPT(
      publicId = URI.create("urn:resourcetype:concept"),
      translations =
          rtTranslations(
              en = "Concept article",
              nb = "Forklaringsartikkel",
              nn = "Forklaringsartikkel",
              se = "Čielggadanartihkkal",
          ),
      order = 5,
  ),
  GAME(
      publicId = URI.create("urn:resourcetype:game"),
      translations =
          rtTranslations(
              en = "Game",
              nb = "Spill",
              nn = "Spel",
              se = "Speala",
          ),
      order = 6,
  ),
  DOCUMENTARY(
      publicId = URI.create("urn:resourcetype:documentary"),
      translations =
          rtTranslations(
              en = "Documentary",
              nb = "Dokumentarfilm",
              nn = "Dokumentarfilm",
              se = "Dokumentára filbma",
          ),
      order = 7,
  ),
  SHORT_FILM(
      publicId = URI.create("urn:resourcetype:shortFilm"),
      translations =
          rtTranslations(
              en = "Short Film",
              nb = "Kortfilm",
              nn = "Kortfilm",
              se = "Oanehisfilbma",
          ),
      order = 8,
  ),
  FEATURE_FILM(
      publicId = URI.create("urn:resourcetype:featureFilm"),
      translations =
          rtTranslations(
              en = "Feature Film",
              nb = "Spillefilm",
              nn = "Spelefilm",
              se = "Guoimmuhanfilbma",
          ),
      order = 9,
  ),
  SERIES(
      publicId = URI.create("urn:resourcetype:series"),
      translations =
          rtTranslations(
              en = "Series",
              nb = "Serier",
              nn = "Seriar",
              se = "Ráiddut",
          ),
      order = 10,
  ),
  ;

  @Throws(NoSuchElementException::class)
  fun getTranslatedName(language: String): String =
      translations.firstOrNull { it.languageCode == language }?.name
          ?: translations.first { it.languageCode == Constants.DefaultLanguage }.name
          ?: throw NoSuchElementException()

  val supportedLanguages = translations.mapNotNull { it.languageCode }.toSet()

  companion object {
    private val BY_PUBLIC_ID = entries.associateBy { it.publicId }

    fun findByPublicId(id: URI) = BY_PUBLIC_ID[id]
  }
}
