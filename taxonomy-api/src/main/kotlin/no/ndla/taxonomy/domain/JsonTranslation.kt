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

class JsonTranslation() : Serializable, Translation {
  @field:JsonProperty("name") override var name: String? = null

  @field:JsonProperty("languageCode") override var languageCode: String? = null

  @field:JsonIgnore @get:JsonIgnore @set:JsonIgnore var parent: Translatable? = null

  constructor(js: JsonTranslation) : this() {
    this.name = js.name
    this.languageCode = js.languageCode
  }

  constructor(languageCode: String?) : this() {
    this.languageCode = languageCode
  }

  constructor(name: String?, languageCode: String?) : this() {
    this.name = name
    this.languageCode = languageCode
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is JsonTranslation) return false
    return name == other.name && languageCode == other.languageCode
  }

  override fun hashCode(): Int {
    var result = name?.hashCode() ?: 0
    result = 31 * result + (languageCode?.hashCode() ?: 0)
    return result
  }
}
