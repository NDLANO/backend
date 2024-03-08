/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain.config

import cats.implicits.toFunctorOps
import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.parser.parse
import io.circe.syntax._
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.api
import no.ndla.myndla.model.domain.config
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import scalikejdbc._

import scala.util.{Failure, Success, Try}

sealed trait ConfigMetaValue
case class BooleanValue(value: Boolean)         extends ConfigMetaValue
case class StringListValue(value: List[String]) extends ConfigMetaValue

object ConfigMetaValue {

  import io.circe.{Decoder, Encoder}, io.circe.generic.auto._

  implicit def encoder: Encoder[ConfigMetaValue] = Encoder.instance {
    case bool @ BooleanValue(_)       => bool.asJson
    case strList @ StringListValue(_) => strList.asJson
  }
  implicit def decoder: Decoder[ConfigMetaValue] = {
    List[Decoder[ConfigMetaValue]](
      Decoder[BooleanValue].widen,
      Decoder[StringListValue].widen
    ).reduceLeft(_ or _)
  }

  def from(configMetaValue: api.config.ConfigMetaValue): ConfigMetaValue = configMetaValue.value match {
    case Left(value)  => config.BooleanValue(value)
    case Right(value) => config.StringListValue(value)
  }

}

case class ConfigMeta(
    key: ConfigKey,
    value: ConfigMetaValue,
    updatedAt: NDLADate,
    updatedBy: String
) {

  def valueToEither: Either[Boolean, List[String]] = {
    value match {
      case BooleanValue(value)    => Left(value)
      case StringListValue(value) => Right(value)
    }
  }

  private def validateBooleanKey(configKey: ConfigKey): Try[ConfigMeta] = {
    value match {
      case BooleanValue(_) => Success(this)
      case _ =>
        val validationMessage = ValidationMessage(
          "value",
          s"Value of '${configKey.entryName}' must be a boolean string ('true' or 'false')"
        )
        Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
    }
  }
  private def validateStringListKey(orgs: ConfigKey): Try[ConfigMeta] = {
    value match {
      case StringListValue(_) => Success(this)
      case _ =>
        val validationMessage = ValidationMessage(
          "value",
          s"Value of '${orgs.entryName}' must be a list of strings"
        )
        Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
    }
  }

  def validate: Try[ConfigMeta] = key match {
    case ConfigKey.LearningpathWriteRestricted => validateBooleanKey(ConfigKey.LearningpathWriteRestricted)
    case ConfigKey.MyNDLAWriteRestricted       => validateBooleanKey(ConfigKey.MyNDLAWriteRestricted)
    case ConfigKey.ArenaEnabledOrgs            => validateStringListKey(ConfigKey.ArenaEnabledOrgs)
    case ConfigKey.ArenaEnabledUsers           => validateStringListKey(ConfigKey.ArenaEnabledUsers)
    case ConfigKey.AiEnabledOrgs               => validateStringListKey(ConfigKey.AiEnabledOrgs)
  }
}

object DBConfigMeta extends SQLSyntaxSupport[ConfigMeta] with StrictLogging {
  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      Json4s.serializer(ConfigKey) ++
      JavaTimeSerializers.all +
      NDLADate.Json4sSerializer

  override val tableName = "configtable"

  def fromResultSet(c: SyntaxProvider[ConfigMeta])(rs: WrappedResultSet): Try[ConfigMeta] =
    fromResultSet(c.resultName)(rs)
  import ConfigMetaValue._

  implicit val enc: Encoder[ConfigMeta] = deriveEncoder[ConfigMeta]
  implicit val dec: Decoder[ConfigMeta] = deriveDecoder[ConfigMeta]

  def fromResultSet(c: ResultName[ConfigMeta])(rs: WrappedResultSet): Try[ConfigMeta] = {
    val dbStr  = rs.string(c.column("value"))
    val parsed = parse(dbStr)
    parsed.flatMap(_.as[ConfigMeta]).toTry
  }

}
