/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import enumeratum.Json4s
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.Props
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class ConfigMeta(
    key: ConfigKey,
    value: String,
    updatedAt: NDLADate,
    updatedBy: String
) {

  private def validateBooleanKey(configKey: ConfigKey): Try[ConfigMeta] = {
    Try(value.toBoolean) match {
      case Success(_) => Success(this)
      case Failure(_) =>
        val validationMessage = ValidationMessage(
          "value",
          s"Value of '${configKey.entryName}' must be a boolean string ('true' or 'false')"
        )
        Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
    }
  }

  def validate: Try[ConfigMeta] = key match {
    case ConfigKey.LearningpathWriteRestricted => validateBooleanKey(ConfigKey.LearningpathWriteRestricted)
    case ConfigKey.MyNDLAWriteRestricted       => validateBooleanKey(ConfigKey.MyNDLAWriteRestricted)
  }
}

trait DBConfigMeta {
  this: Props =>
  object DBConfigMeta extends SQLSyntaxSupport[ConfigMeta] {
    implicit val formats: Formats =
      org.json4s.DefaultFormats +
        Json4s.serializer(ConfigKey) ++
        JavaTimeSerializers.all +
        NDLADate.Json4sSerializer

    override val tableName  = "configtable"
    override val schemaName = Some(props.MetaSchema)

    def fromResultSet(c: SyntaxProvider[ConfigMeta])(rs: WrappedResultSet): ConfigMeta = fromResultSet(c.resultName)(rs)

    def fromResultSet(c: ResultName[ConfigMeta])(rs: WrappedResultSet): ConfigMeta = {
      val meta = read[ConfigMeta](rs.string(c.column("value")))
      meta
    }
  }

}
