/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.config

import cats.implicits.toFunctorOps
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.parser.parse
import io.circe.syntax.*
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.{NDLADate, api}
import no.ndla.common.model.domain.config
import scalikejdbc.*

import scala.annotation.unused
import scala.util.{Failure, Success, Try}

sealed trait ConfigMetaValue
case class BooleanValue(value: Boolean)         extends ConfigMetaValue
case class StringListValue(value: List[String]) extends ConfigMetaValue

object ConfigMetaValue {

  import io.circe.{Decoder, Encoder}, io.circe.generic.auto.*

  implicit def encoder: Encoder[ConfigMetaValue] = Encoder.instance {
    case bool @ BooleanValue(_)       => bool.asJson
    case strList @ StringListValue(_) => strList.asJson
  }
  implicit def decoder: Decoder[ConfigMetaValue] = {
    List[Decoder[ConfigMetaValue]](Decoder[BooleanValue].widen, Decoder[StringListValue].widen).reduceLeft(_ or _)
  }

  def from(configMetaValue: api.config.ConfigMetaValueDTO): ConfigMetaValue = configMetaValue.value match {
    case Left(value)  => config.BooleanValue(value)
    case Right(value) => config.StringListValue(value)
  }

}

case class ConfigMeta(key: ConfigKey, value: ConfigMetaValue, updatedAt: NDLADate, updatedBy: String) {

  def valueToEither: Either[Boolean, List[String]] = {
    value match {
      case BooleanValue(value)    => Left(value)
      case StringListValue(value) => Right(value)
    }
  }

  private def validateBooleanKey(configKey: ConfigKey): Try[ConfigMeta] = {
    value match {
      case BooleanValue(_) => Success(this)
      case _               =>
        val validationMessage =
          ValidationMessage("value", s"Value of '${configKey.entryName}' must be a boolean string ('true' or 'false')")
        Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
    }
  }
  @unused // for now
  private def validateStringListKey(configKey: ConfigKey): Try[ConfigMeta] = {
    value match {
      case StringListValue(_) => Success(this)
      case _                  =>
        val validationMessage =
          ValidationMessage("value", s"Value of '${configKey.entryName}' must be a list of strings")
        Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
    }
  }

  def validate: Try[ConfigMeta] = key match {
    case ConfigKey.MyNDLAWriteRestricted => validateBooleanKey(ConfigKey.MyNDLAWriteRestricted)
  }
}

object ConfigMeta extends SQLSyntaxSupport[ConfigMeta] with StrictLogging {
  override val tableName = "configtable"

  def fromResultSet(c: SyntaxProvider[ConfigMeta])(rs: WrappedResultSet): Try[ConfigMeta] =
    fromResultSet(c.resultName)(rs)

  implicit val enc: Encoder[ConfigMeta] = deriveEncoder[ConfigMeta]
  implicit val dec: Decoder[ConfigMeta] = deriveDecoder[ConfigMeta]

  def fromResultSet(c: ResultName[ConfigMeta])(rs: WrappedResultSet): Try[ConfigMeta] = {
    val dbStr  = rs.string(c.column("value"))
    val parsed = parse(dbStr)
    parsed.flatMap(_.as[ConfigMeta]).toTry
  }

}
