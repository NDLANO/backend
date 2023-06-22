/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.search

import com.sksamuel.elastic4s.fields.{ElasticField, NestedField, ObjectField}
import org.json4s.JsonAST.{JArray, JObject, JValue}

import scala.annotation.tailrec

object TestUtility {
  @tailrec
  private def getArrayFields(json: JArray, prefix: String): Seq[String] = {
    val firstElement =
      json.arr.headOption.getOrElse(
        throw new RuntimeException(s"Array '$prefix' seems to be empty, this makes checking subfields hard")
      )
    firstElement match {
      case obj: JObject =>
        getFields(obj, Some(prefix))
      case arr: JArray =>
        getArrayFields(arr, s"$prefix[0]")
      case _ => Seq.empty
    }
  }

  def getFields(json: JValue, prefix: Option[String]): Seq[String] = {
    val pre = prefix.map(x => s"$x.").getOrElse("")
    json match {
      case arr: JArray =>
        getArrayFields(arr, s"$pre")
      case JObject(obj) =>
        obj.foldLeft(List.empty[String]) {
          case (acc, (name, value: JObject)) =>
            val fix       = s"$pre$name"
            val subfields = getFields(value, Some(fix))
            acc ++ subfields
          case (acc, (name, value: JArray)) =>
            val fix       = s"$pre$name"
            val subfields = getArrayFields(value, fix)
            acc ++ subfields
          case (acc, (name, _)) =>
            val fix = s"$pre$name"
            acc :+ fix
        }
      case _ => List.empty
    }
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
