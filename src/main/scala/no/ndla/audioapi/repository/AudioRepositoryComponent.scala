/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.DataSourceComponent
import no.ndla.audioapi.model.domain.AudioMetaInformation
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{DBSession, ReadOnlyAutoSession, _}


trait AudioRepositoryComponent {
  this: DataSourceComponent =>
  val audioRepository: AudioRepository

  class AudioRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats + AudioMetaInformation.JSonSerializer

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def withId(id: Long): Option[AudioMetaInformation] = {
      DB readOnly { implicit session =>
        audioMetaInformationWhere(sqls"au.id = $id")
      }
    }

    def withExternalId(externalId: String): Option[AudioMetaInformation] = {
      DB readOnly { implicit session =>
        audioMetaInformationWhere(sqls"au.external_id = $externalId")
      }
    }

    def insert(audioMetaInformation: AudioMetaInformation, externalId: String): AudioMetaInformation = {
      val json = write(audioMetaInformation)

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        val audioId = sql"insert into audiometadata(external_id, metadata) values(${externalId}, ${dataObject})".updateAndReturnGeneratedKey.apply
        audioMetaInformation.copy(id = Some(audioId))
      }
    }

    def update(audioMetaInformation: AudioMetaInformation, id: Long): AudioMetaInformation = {
      val json = write(audioMetaInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        sql"update audiometadata set metadata = ${dataObject} where id = ${id}".update.apply
        audioMetaInformation.copy(id = Some(id))
      }
    }

    def numElements: Int = {
      DB readOnly { implicit session =>
        sql"select count(*) from audiometadata".map(rs => {
          rs.int("count")
        }).list.first().apply() match {
          case Some(count) => count
          case None => 0
        }
      }
    }

    def applyToAll(func: List[AudioMetaInformation] => Unit) = {
      val au = AudioMetaInformation.syntax("au")
      val numberOfBulks = math.ceil(numElements.toFloat / AudioApiProperties.IndexBulkSize).toInt

      DB readOnly { implicit session =>
        for(i <- 0 until numberOfBulks) {
          func(
            sql"""select ${au.result.*} from ${AudioMetaInformation.as(au)} limit ${AudioApiProperties.IndexBulkSize} offset ${i * AudioApiProperties.IndexBulkSize}""".map(AudioMetaInformation(au)).list.apply()
          )
        }
      }
    }

    private def audioMetaInformationWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[AudioMetaInformation] = {
      val au = AudioMetaInformation.syntax("au")
      sql"select ${au.result.*} from ${AudioMetaInformation.as(au)} where $whereClause".map(AudioMetaInformation(au)).single().apply()
    }

  }
}
