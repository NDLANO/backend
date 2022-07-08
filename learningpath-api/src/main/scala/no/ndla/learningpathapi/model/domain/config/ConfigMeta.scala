/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import enumeratum.Json4s
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.ValidationException
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization._
import scalikejdbc.{WrappedResultSet, _}

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

case class ConfigMeta(
    key: ConfigKey,
    value: String,
    updatedAt: LocalDateTime,
    updatedBy: String
) {

  def validate: Try[ConfigMeta] = {
    key match {
      case ConfigKey.IsWriteRestricted =>
        Try(value.toBoolean) match {
          case Success(_) => Success(this)
          case Failure(_) =>
            val validationMessage = ValidationMessage(
              "value",
              s"Value of '${ConfigKey.IsWriteRestricted.entryName}' must be a boolean string ('true' or 'false')"
            )
            Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
        }
      // Add case here for validation for new ConfigKeys
    }
  }
}

trait DBConfigMeta {
  this: Props =>
  object DBConfigMeta extends SQLSyntaxSupport[ConfigMeta] {
    implicit val formats: Formats = org.json4s.DefaultFormats +
      Json4s.serializer(ConfigKey) ++ JavaTimeSerializers.all

    override val tableName  = "configtable"
    override val schemaName = Some(props.MetaSchema)

    def fromResultSet(c: SyntaxProvider[ConfigMeta])(rs: WrappedResultSet): ConfigMeta = fromResultSet(c.resultName)(rs)

    def fromResultSet(c: ResultName[ConfigMeta])(rs: WrappedResultSet): ConfigMeta = {
      val meta = read[ConfigMeta](rs.string(c.column("value")))
      meta
    }
  }

}
