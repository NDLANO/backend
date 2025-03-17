/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common

import sttp.tapir.{FieldName, Schema, SchemaType}

object TapirUtil {
  def withDiscriminator[T](schema: Schema[T]): Schema[T] = {
    val schemaType: SchemaType[T] = schema.schemaType match {
      case st: SchemaType.SProduct[T] =>
        val newField = SchemaType.SProductField[T, String](
          FieldName("typename"),
          Schema.string.description("Discriminator field"),
          _ => throwNewError(schema)
        )
        st.copy(fields = st.fields :+ newField)
      case x => x
    }
    schema.copy(schemaType = schemaType)
  }

  def throwNewError[T](schema: Schema[T]): Nothing = {
    throw new RuntimeException(s"Attempted to get typename from a value of type $schema")
  }

}
