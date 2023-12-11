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
import no.ndla.common.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.{DBMyNDLAUser, MyNDLAUser}

trait ArenaRepository {

  val arenaRepository: ArenaRepository

  class ArenaRepository {

    def postTopic(categoryId: Long, title: String, ownerId: Long, created: NDLADate)(implicit
        session: DBSession
    ): Try[domain.Topic] = Try {
      val column = domain.Topic.column.c _
      val inserted = withSQL {
        insert
          .into(domain.Topic)
          .namedValues(
            column("title")       -> title,
            column("category_id") -> categoryId,
            column("owner_id")    -> ownerId,
            column("created")     -> created,
            column("updated")     -> created
          )
      }.updateAndReturnGeneratedKey
        .apply()

      domain.Topic(
        id = inserted,
        ownerId = ownerId,
        title = title,
        category_id = categoryId,
        created = created,
        updated = created
      )

    }

    def postPost(topicId: Long, content: String, ownerId: Long)(implicit session: DBSession): Try[domain.Post] = Try {
      val created = NDLADate.now()
      val column  = domain.Post.column.c _
      val inserted = withSQL {
        insert
          .into(domain.Post)
          .namedValues(
            column("topic_id") -> topicId,
            column("owner_id") -> ownerId,
            column("content")  -> content,
            column("created")  -> created,
            column("updated")  -> created
          )
      }.updateAndReturnGeneratedKey
        .apply()

      domain.Post(
        id = inserted,
        content = content,
        topic_id = topicId,
        created = created,
        updated = created,
        ownerId = ownerId
      )
    }

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

    def getTopic(
        topicId: Long
    )(implicit session: DBSession): Try[Option[(domain.Topic, List[(domain.Post, MyNDLAUser)])]] = {
      val t = domain.Topic.syntax("t")
      val p = domain.Post.syntax("p")
      val u = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
             select ${t.resultAll}, ${p.resultAll}, ${u.resultAll}
             from ${domain.Topic.as(t)}
             left join ${domain.Post.as(p)} ON ${p.topic_id} = ${t.id}
             left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${p.ownerId}
             where ${t.id} = $topicId
           """
          .one(rs => domain.Topic.fromResultSet(t.resultName)(rs))
          .toManies(
            rs => domain.Post.fromResultSet(p.resultName)(rs).toOption,
            rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption
          )
          .map((topic, posts, owners) => combine(topic, posts.toSeq, owners.toSeq))
          .single
          .apply()
          .sequence
      }.flatten
    }

    def combine(
        topic: Try[domain.Topic],
        posts: Seq[domain.Post],
        owners: Seq[MyNDLAUser]
    ): Try[(domain.Topic, List[(domain.Post, MyNDLAUser)])] = {
      for {
        t <- topic
        postsWithOwners <- posts.toList.traverse(post =>
          owners
            .find(_.id == post.ownerId)
            .toTry(new RuntimeException(s"Post id ${post.id} with no owner, this seems like a data inconsistency bug."))
            .map(owner => (post, owner))
        )
      } yield (t, postsWithOwners)
    }

    def getTopicsForCategory(
        categoryId: Long
    )(implicit session: DBSession): Try[List[(domain.Topic, List[(domain.Post, MyNDLAUser)])]] = {
      val t = domain.Topic.syntax("t")
      val p = domain.Post.syntax("p")
      val u = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
             select ${t.resultAll}, ${p.resultAll}, ${u.resultAll}
             from ${domain.Topic.as(t)}
             left join ${domain.Post.as(p)} on ${p.topic_id} = ${t.id}
             left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${p.ownerId}
             where ${t.category_id} = $categoryId
           """
          .one(rs => domain.Topic.fromResultSet(t.resultName)(rs))
          .toManies(
            rs => domain.Post.fromResultSet(p.resultName)(rs).toOption,
            rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption
          )
          .map((topic, posts, owners) => combine(topic, posts.toSeq, owners.toSeq))
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
