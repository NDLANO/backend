/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.repository

import scalikejdbc._

import scala.util.Try
import no.ndla.myndlaapi.model.arena.domain
import cats.implicits._

trait ArenaRepository {

  val arenaRepository: ArenaRepository

  class ArenaRepository {

    def withSession[T](func: DBSession => T): T = {
      DB.localTx { session =>
        func(session)
      }
    }

    def getCategory(id: Long)(implicit session: DBSession): Try[Option[domain.Category]] = {
      val ca = domain.Category.syntax("ca")
      Try {
        sql"""
             select ${ca.resultAll}
             from ${domain.Category.as(ca)}
             where ${ca.id} = $id
             """
          .map(rs => domain.Category.fromResultSet(ca)(rs))
          .single
          .apply()
          .sequence
      }.flatten
    }

    def getTopicsForCategory(
        categoryId: Long
    )(implicit session: DBSession): Try[List[(domain.Topic, List[domain.Post])]] = {
      val t = domain.Topic.syntax("t")
      val p = domain.Post.syntax("p")
      Try {
        sql"""
                 select ${t.resultAll}, ${p.resultAll}
                 from ${domain.Topic.as(t)}
                 left join ${domain.Post.as(p)} ON ${p.topicId} = ${t.id}
                 where ${t.category_id} = $categoryId
                 """
          .one(rs => domain.Topic.fromResultSet(t.resultName)(rs))
          .toMany(rs => domain.Post.fromResultSet(p.resultName)(rs).toOption)
          .map((topic, posts) => topic.map(t => (t, posts.toList)))
          .list
          .apply()
          .sequence
      }.flatten
    }

    def getCategories(implicit session: DBSession): Try[List[domain.Category]] = {
      val ca = domain.Category.syntax("ca")
      Try {
        sql"""
             select ${ca.resultAll}
             from ${domain.Category.as(ca)}
             """
          .map(rs => domain.Category.fromResultSet(ca)(rs))
          .list
          .apply()
          .sequence
      }.flatten
    }

    def getPostCountForCategory(categoryId: Long)(implicit session: DBSession): Try[Long] = {
      Try {
        sql"""
             select count(*) from posts p
             where p.topic_id in (
              select id
              from topics t
              where t.category_id = $categoryId
             )
             """
          .map(rs => rs.long("count"))
          .single
          .apply()
          .getOrElse(0L)
      }
    }

    def getTopicCountForCategory(categoryId: Long)(implicit session: DBSession): Try[Long] = {
      Try {
        sql"""
             select count(*) as count
             from topics
             where category_id = $categoryId
             """
          .map(rs => rs.long("count"))
          .single
          .apply()
          .getOrElse(0L)
      }
    }

    def insertCategory(
        category: domain.InsertCategory
    )(implicit session: DBSession): Try[domain.Category] = Try {
      val id =
        sql"""
            insert into ${domain.Category.table}
              (title, description)
            values (
              ${category.title},
              ${category.description},
            )
           """.updateAndReturnGeneratedKey
          .apply()
      category.toFull(id)
    }

  }
}
