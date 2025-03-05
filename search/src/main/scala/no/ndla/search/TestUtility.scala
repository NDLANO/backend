/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import com.sksamuel.elastic4s.fields.{ElasticField, NestedField, ObjectField}
import io.circe.Json

object TestUtility {
  private def getArrayFields(json: Vector[Json], prefix: String, skipFields: Seq[String]): Seq[String] = {
    val firstElement = json.headOption.getOrElse(
      throw new RuntimeException(s"Array '$prefix' seems to be empty, this makes checking subfields hard")
    )

    firstElement.arrayOrObject(
      or = { Seq.empty },
      jsonObject = { obj => getFields(obj.toJson, Some(prefix), skipFields) },
      jsonArray = { arr => getArrayFields(arr, s"$prefix[0]", skipFields) }
    )
  }

  def getFields(json: Json, prefix: Option[String], skipFields: Seq[String] = Seq.empty): Seq[String] = {
    val pre = prefix.map(x => s"$x.").getOrElse("")

    json.asArray match {
      case Some(value) => return getArrayFields(value, s"$pre", skipFields)
      case _           =>
    }

    json.arrayOrObject(
      or = { List.empty },
      jsonArray = { arr => getArrayFields(arr, s"$pre", skipFields) },
      jsonObject = { obj =>
        obj.toMap.foldLeft(List.empty[String]) {
          case (acc, (name, value)) if value.isObject =>
            if (skipFields.contains(name) || skipFields.contains(s"$pre$name")) acc
            else {
              val fix       = s"$pre$name"
              val subfields = getFields(value, Some(fix), skipFields)
              acc ++ subfields
            }
          case (acc, (name, value)) if value.isArray =>
            if (skipFields.contains(name) || skipFields.contains(s"$pre$name")) acc
            else {
              val fix       = s"$pre$name"
              val subfields = getArrayFields(value.asArray.getOrElse(Vector.empty), fix, skipFields)
              acc ++ subfields
            }
          case (acc, (name, _)) =>
            if (skipFields.contains(name) || skipFields.contains(s"$pre$name")) acc
            else {
              val fix = s"$pre$name"
              acc :+ fix
            }
        }
      }
    )
  }

  def getMappingFields(fields: Seq[ElasticField], prefix: Option[String]): Seq[String] = {
    val pre = prefix.map(x => s"$x.").getOrElse("")
    val names = fields.flatMap {
      case nf: NestedField =>
        val prefix = Some(s"$pre${nf.name}")
        getMappingFields(nf.properties, prefix)
      case of: ObjectField =>
        val prefix = Some(s"$pre${of.name}")
        getMappingFields(of.properties, prefix)
      case f =>
        Seq(s"$pre${f.name}")
    }
    names
  }

}
