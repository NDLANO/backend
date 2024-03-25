/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.repository

import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.StrictLogging
import io.circe.syntax._
import no.ndla.myndla.model.domain.config.{ConfigKey, ConfigMeta}
import org.postgresql.util.PGobject
import scalikejdbc._
import sqls.count

import scala.util.{Success, Try}

trait ConfigRepository {
  val configRepository: ConfigRepository

  import ConfigMeta._

  class ConfigRepository extends StrictLogging {
    implicit val configValueParameterBinderFactory: ParameterBinderFactory[ConfigMeta] =
      ParameterBinderFactory[ConfigMeta] { value => (stmt, idx) =>
        {
          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(value.asJson.noSpaces)
          stmt.setObject(idx, dataObject)
        }
      }

    def configCount(implicit session: DBSession = ReadOnlyAutoSession): Int = {
      val c = ConfigMeta.syntax("c")
      withSQL {
        select(count(c.column("configkey"))).from(ConfigMeta as c)
      }.map(_.int(1)).single().getOrElse(0)
    }

    def updateConfigParam(config: ConfigMeta)(implicit session: DBSession = AutoSession): Try[ConfigMeta] = {
      val updatedCount = withSQL {
        update(ConfigMeta)
          .set(ConfigMeta.column.column("value") -> config)
          .where
          .eq(ConfigMeta.column.column("configkey"), config.key.entryName)
      }.update()

      if (updatedCount != 1) {
        logger.info(s"No existing value for ${config.key}, inserting the value.")
        withSQL {
          insertInto(ConfigMeta).namedValues(
            ConfigMeta.column.c("configkey") -> config.key.entryName,
            ConfigMeta.column.c("value")     -> config
          )
        }.update(): Unit
        Success(config)
      } else {
        logger.info(s"Value for ${config.key} updated.")
        Success(config)
      }
    }

    def getConfigWithKey(key: ConfigKey)(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[ConfigMeta]] =
      Try {
        val keyName = key.entryName
        val c       = ConfigMeta.syntax("c")
        sql"""
           select ${c.result.*}
           from ${ConfigMeta.as(c)}
           where configkey = $keyName;
        """
          .map(ConfigMeta.fromResultSet(c))
          .single()
          .sequence
      }.flatten
  }
}
