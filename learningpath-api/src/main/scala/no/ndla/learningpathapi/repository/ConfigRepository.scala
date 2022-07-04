/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta, DBConfigMeta}
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._
import sqls.count

import scala.util.{Success, Try}

trait ConfigRepository {
  this: DataSource with DBConfigMeta =>
  val configRepository: ConfigRepository

  class ConfigRepository extends LazyLogging {
    implicit val formats: Formats = DBConfigMeta.formats ++ JavaTimeSerializers.all
    implicit val configValueParameterBinderFactory: ParameterBinderFactory[ConfigMeta] =
      ParameterBinderFactory[ConfigMeta] { value => (stmt, idx) =>
        {
          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(write(value))
          stmt.setObject(idx, dataObject)
        }
      }

    def configCount(implicit session: DBSession = ReadOnlyAutoSession): Int = {
      val c = DBConfigMeta.syntax("c")
      withSQL {
        select(count(c.column("configkey"))).from(DBConfigMeta as c)
      }.map(_.int(1)).single().getOrElse(0)
    }

    def updateConfigParam(config: ConfigMeta)(implicit session: DBSession = AutoSession): Try[ConfigMeta] = {
      val updatedCount = withSQL {
        update(DBConfigMeta)
          .set(DBConfigMeta.column.column("value") -> config)
          .where
          .eq(DBConfigMeta.column.column("configkey"), config.key.entryName)
      }.update()

      if (updatedCount != 1) {
        logger.info(s"No existing value for ${config.key}, inserting the value.")
        withSQL {
          insertInto(DBConfigMeta).namedValues(
            DBConfigMeta.column.c("configkey") -> config.key.entryName,
            DBConfigMeta.column.c("value")     -> config
          )
        }.update()
        Success(config)
      } else {
        logger.info(s"Value for ${config.key} updated.")
        Success(config)
      }
    }

    def getConfigWithKey(key: ConfigKey)(implicit session: DBSession = ReadOnlyAutoSession): Option[ConfigMeta] = {
      val keyName = key.entryName
      val c       = DBConfigMeta.syntax("c")
      sql"""
           select ${c.result.*}
           from ${DBConfigMeta.as(c)}
           where configkey = $keyName;
        """
        .map(DBConfigMeta.fromResultSet(c))
        .single()
    }
  }
}
