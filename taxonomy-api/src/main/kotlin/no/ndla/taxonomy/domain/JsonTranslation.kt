/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.util.Objects

class JsonTranslation : Serializable, Translation {
  @field:JsonProperty("name") override var name: String? = null

  @field:JsonProperty("languageCode") override var languageCode: String? = null

  @field:JsonIgnore @get:JsonIgnore @set:JsonIgnore var parent: Translatable? = null

  constructor()

  constructor(js: JsonTranslation) {
    this.name = js.name
    this.languageCode = js.languageCode
  }

  constructor(languageCode: String?) {
    this.languageCode = languageCode
  }

  constructor(name: String?, languageCode: String?) {
    this.name = name
    this.languageCode = languageCode
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as JsonTranslation
    return Objects.equals(this.languageCode, that.languageCode) &&
        Objects.equals(this.name, that.name)
  }

  override fun hashCode(): Int {
    return Objects.hash(this.name, this.languageCode)
  }
}
