/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.repository

import scalikejdbc._

import scala.util.{Failure, Success, Try}
import no.ndla.myndlaapi.model.arena.domain
import cats.implicits._
import no.ndla.common.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.{DBMyNDLAUser, MyNDLAUser, NDLASQLException}
import no.ndla.myndlaapi.model.arena.domain.{Post, Topic}

trait ArenaRepository {

  val arenaRepository: ArenaRepository

  class ArenaRepository {
    def flagPost(flagger: MyNDLAUser, postId: Long, reason: String, created: NDLADate)(implicit
        session: DBSession
    ): Try[domain.Flag] = Try {
      val column = domain.Flag.column.c _

      val inserted = withSQL {
        insert
          .into(domain.Flag)
          .namedValues(
            column("user_id")  -> flagger.id,
            column("post_id")  -> postId,
            column("reason")   -> reason,
            column("created")  -> created,
            column("resolved") -> None
          )
      }.updateAndReturnGeneratedKey.apply()

      domain.Flag(
        id = inserted,
        user_id = flagger.id,
        post_id = postId,
        reason = reason,
        created = created,
        resolved = None
      )
    }

    def deleteCategory(categoryId: Long)(implicit session: DBSession): Try[Int] = Try {
      val c = domain.Category.syntax("c")
      val count = withSQL {
        delete
          .from(domain.Category as c)
          .where
          .eq(c.id, categoryId)
      }.update()
      if (count < 1) Failure(NDLASQLException(s"Deleting a category with id '$categoryId' resulted in no affected row"))
      else Success(count)
    }.flatten

    def deleteTopic(topicId: Long)(implicit session: DBSession): Try[Int] = Try {
      val t = domain.Topic.syntax("t")
      val count = withSQL {
        delete
          .from(Topic as t)
          .where
          .eq(t.id, topicId)
      }.update()
      if (count < 1) Failure(NDLASQLException(s"Deleting a topic with id '$topicId' resulted in no affected row"))
      else Success(count)
    }.flatten

    def deletePost(postId: Long)(implicit session: DBSession): Try[Int] = Try {
      val p = domain.Post.syntax("p")
      val count = withSQL {
        delete
          .from(Post as p)
          .where
          .eq(p.id, postId)
      }.update()
      if (count < 1) Failure(NDLASQLException(s"Deleting a post with id '$postId' resulted in no affected row"))
      else Success(count)
    }.flatten

    def getPost(postId: Long)(implicit session: DBSession): Try[Option[domain.Post]] = {
      val p = domain.Post.syntax("p")
      Try {
        sql"""
                 select ${p.resultAll}
                 from ${domain.Post.as(p)}
                 where ${p.id} = $postId
             """
          .map(rs => domain.Post.fromResultSet(p.resultName)(rs))
          .single
          .apply()
          .sequence
      }.flatten
    }

    def insertTopic(categoryId: Long, title: String, ownerId: Long, created: NDLADate)(implicit
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

    def updateTopic(topicId: Long, title: String, updated: NDLADate)(implicit session: DBSession): Try[domain.Topic] =
      Try {
        val column = domain.Topic.column.c _
        withSQL {
          update(domain.Topic)
            .set(
              column("title")   -> title,
              column("updated") -> updated
            )
            .where
            .eq(domain.Topic.column.id, topicId)
            .append(sqls"returning ${sqls.csv(domain.Topic.column.*)}")
        }
          .map(rs => domain.Topic.fromResultSet(column)(rs))
          .single
          .apply() match {
          case Some(topic) => topic
          case None        => Failure(NDLASQLException(s"This is a Bug! Updating a topic resulted in no returned row"))
        }
      }.flatten

    def updatePost(postId: Long, content: String, updated: NDLADate)(implicit session: DBSession): Try[domain.Post] = {
      Try {
        val column = domain.Post.column.c _
        withSQL {
          update(domain.Post)
            .set(
              column("content") -> content,
              column("updated") -> updated
            )
            .where
            .eq(domain.Post.column.id, postId)
            .append(sqls"returning ${sqls.csv(domain.Post.column.*)}")
        }
          .map(rs => domain.Post.fromResultSet(column)(rs))
          .single
          .apply() match {
          case Some(post) => post
          case None       => Failure(NDLASQLException(s"This is a Bug! Updating a post resulted in no returned row"))
        }
      }.flatten
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
      } yield (t, postsWithOwners.sortBy(x => x._1.created))
    }

    def topicCount(implicit session: DBSession): Try[Long] = Try {
      sql"""
           select count(*) as count
           from ${domain.Topic.table}
         """
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0L)
    }

    def getTopicsPaginated(offset: Long, limit: Long)(implicit
        session: DBSession
    ): Try[List[(Topic, List[(Post, MyNDLAUser)])]] = {

      val t  = domain.Topic.syntax("t")
      val ts = SubQuery.syntax("ts").include(t)
      val p  = domain.Post.syntax("p")
      val u  = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
              select ${ts.resultAll}, ${p.resultAll}, ${u.resultAll}
              from (
                  select ${t.resultAll}, (select max(pp.created) from posts pp where pp.topic_id = ${t.id}) as newest_post_date
                  from ${domain.Topic.as(t)}
                  order by newest_post_date desc nulls last
                  limit $limit
                  offset $offset
                ) ts
               left join ${domain.Post.as(p)} on ${p.topic_id} = ${ts(t).id}
               left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${p.ownerId}
               order by newest_post_date desc nulls last
           """
          .one(rs => domain.Topic.fromResultSet(ts(t).resultName)(rs))
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

    def getTopicsForCategory(categoryId: Long, offset: Long, limit: Long)(implicit
        session: DBSession
    ): Try[List[(domain.Topic, List[(domain.Post, MyNDLAUser)])]] = {
      val t  = domain.Topic.syntax("t")
      val ts = SubQuery.syntax("ts").include(t)
      val p  = domain.Post.syntax("p")
      val u  = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
              select ${ts.resultAll}, ${p.resultAll}, ${u.resultAll}
              from (
                  select ${t.resultAll}, (select max(pp.created) from posts pp where pp.topic_id = ${t.id}) as newest_post_date
                  from ${domain.Topic.as(t)}
                  where ${t.category_id} = $categoryId
                  order by newest_post_date desc nulls last
                  limit $limit
                  offset $offset
                ) ts
               left join ${domain.Post.as(p)} on ${p.topic_id} = ${ts(t).id}
               left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${p.ownerId}
               order by newest_post_date desc nulls last
           """
          .one(rs => domain.Topic.fromResultSet(ts(t).resultName)(rs))
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
              ${category.description}
            )
           """.updateAndReturnGeneratedKey
          .apply()
      category.toFull(id)
    }

    def updateCategory(categoryId: Long, category: domain.InsertCategory)(implicit
        session: DBSession
    ): Try[domain.Category] = Try {
      withSQL {
        update(domain.Category)
          .set(
            domain.Category.column.title       -> category.title,
            domain.Category.column.description -> category.description
          )
          .where
          .eq(domain.Category.column.id, categoryId)

      }.update()
    } match {
      case Failure(ex)                  => Failure(ex)
      case Success(count) if count == 1 => Success(category.toFull(categoryId))
      case Success(count) =>
        Failure(NDLASQLException(s"This is a Bug! The expected rows count should be 1 and was $count."))
    }

  }
}
