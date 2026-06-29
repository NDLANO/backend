/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config

import com.fasterxml.jackson.databind.module.SimpleModule
import no.ndla.taxonomy.domain.UpdateOrDelete
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
  @Bean
  fun updateOrDeleteModule() =
      SimpleModule().apply {
        addDeserializer(UpdateOrDelete::class.java, UpdateOrDelete.Deserializer())
        addSerializer(UpdateOrDelete::class.java, UpdateOrDelete.Serializer())
      }
}
