/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.ContextualDeserializer

sealed class UpdateOrDelete<out T> {
  data object Default : UpdateOrDelete<Nothing>()

  data class Update<T>(val value: T) : UpdateOrDelete<T>()

  data object Delete : UpdateOrDelete<Nothing>()

  class Serializer : JsonSerializer<UpdateOrDelete<*>>() {
    override fun isEmpty(provider: SerializerProvider, value: UpdateOrDelete<*>) = value is Default

    override fun serialize(
        value: UpdateOrDelete<*>,
        gen: JsonGenerator,
        provider: SerializerProvider,
    ) =
        when (value) {
          is Delete -> gen.writeNull()
          is Update<*> -> gen.writeObject(value.value)
          is Default -> {}
        }
  }

  class Deserializer(private val innerType: JavaType? = null) :
      JsonDeserializer<UpdateOrDelete<*>>(), ContextualDeserializer {
    override fun createContextual(
        ctxt: DeserializationContext,
        property: BeanProperty?,
    ): JsonDeserializer<*> =
        Deserializer(property?.type?.containedType(0) ?: ctxt.contextualType?.containedType(0))

    override fun getNullValue(ctxt: DeserializationContext?) = Delete

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UpdateOrDelete<*> {
      val node: JsonNode = p.codec.readTree(p)
      return when {
        node.isMissingNode -> Default
        node.isNull -> Delete
        else ->
            Update(
                ctxt.readTreeAsValue<Any>(
                    node,
                    innerType
                        ?: throw JsonMappingException.from(
                            ctxt,
                            "UpdateOrDelete deserializer used without createContextual",
                        ),
                ))
      }
    }
  }
}
