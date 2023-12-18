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
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.implicits.OptionImplicit
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.{DBMyNDLAUser, MyNDLAUser, NDLASQLException}
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledFlag, CompiledNotification, CompiledPost, CompiledTopic}
import no.ndla.myndlaapi.model.arena.domain.{Notification, Post, Topic}

trait ArenaRepository {
  this: Clock =>

  val arenaRepository: ArenaRepository

  class ArenaRepository extends StrictLogging {

    def compileNotification(
        notification: Try[domain.Notification],
        posts: List[domain.Post],
        topics: List[domain.Topic],
        owners: List[MyNDLAUser],
        flags: List[domain.Flag]
    ): Try[CompiledNotification] = {
      notification.flatMap(not => {
        for {
          owner <- owners
            .find(_.id == not.user_id)
            .toTry(NDLASQLException(s"Notification with id ${not.id} has no user ${not.user_id} in result."))
          notificationPost <- posts
            .find(_.id == not.post_id)
            .toTry(NDLASQLException(s"Notification with id ${not.id} has no post ${not.post_id} in result."))
          compiledPost <- compilePost(notificationPost, owners, flags)
          notificationTopic <- topics
            .find(_.id == not.topic_id)
            .toTry(NDLASQLException(s"Notification with id ${not.id} has no topic ${not.topic_id} in result."))
          topicOwner <- owners
            .find(_.id == notificationTopic.ownerId)
            .toTry(NDLASQLException(s"Topic with id ${notificationTopic.id} has no owner in result."))
          compiledTopic = CompiledTopic(notificationTopic, topicOwner)
        } yield CompiledNotification(
          notification = not,
          post = compiledPost,
          topic = compiledTopic,
          notifiedUser = owner
        )
      })
    }

    def getNotifications(user: MyNDLAUser, offset: Long, limit: Long)(implicit session: DBSession) = {
      val n  = domain.Notification.syntax("n")
      val ns = SubQuery.syntax("ns").include(n)
      val p  = domain.Post.syntax("p")
      val t  = domain.Topic.syntax("t")
      val u  = DBMyNDLAUser.syntax("u")
      val f  = domain.Flag.syntax("f")
      Try {
        sql"""
             select ${ns.resultAll}, ${p.resultAll}, ${t.resultAll}, ${u.resultAll}, ${f.resultAll}
             from (
              select ${n.resultAll}
              from ${domain.Notification.as(n)}
              where ${n.user_id} = ${user.id}
              order by ${n.notification_time} desc
              limit $limit
              offset $offset
             ) ns
             left join ${domain.Post.as(p)} on ${p.id} = ${ns(n).post_id}
             left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${p.ownerId}
             left join ${domain.Topic.as(t)} on ${t.id} = ${ns(n).topic_id}
             left join ${domain.Flag.as(f)} on ${f.post_id} = ${p.id} and ${f.resolved} is null
             order by ${ns(n).notification_time} desc
           """
          .one(rs => domain.Notification.fromResultSet(ns(n).resultName)(rs))
          .toManies(
            rs => domain.Post.fromResultSet(p.resultName)(rs).toOption,
            rs => domain.Topic.fromResultSet(t.resultName)(rs).toOption,
            rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption,
            rs => domain.Flag.fromResultSet(f)(rs).toOption
          )
          .map { (notification, post, topic, owner, flag) =>
            compileNotification(notification, post.toList, topic.toList, owner.toList, flag.toList)
          }
          .list
          .apply()
          .sequence
      }.flatten

    }

    def insertNotification(userId: Long, postId: Long, topicId: Long, notificationTime: NDLADate)(implicit
        session: DBSession
    ): Try[Notification] = Try {
      val column = domain.Notification.column.c _

      val inserted = withSQL {
        insert
          .into(domain.Notification)
          .namedValues(
            column("user_id")           -> userId,
            column("post_id")           -> postId,
            column("topic_id")          -> topicId,
            column("is_read")           -> false,
            column("notification_time") -> notificationTime
          )
      }.updateAndReturnGeneratedKey.apply()

      domain.Notification(
        id = inserted,
        user_id = userId,
        post_id = postId,
        topic_id = topicId,
        is_read = false,
        notification_time = notificationTime
      )
    }

    def readNotifications(userId: Long)(implicit session: DBSession): Try[Unit] = Try {
      val count = withSQL {
        update(domain.Notification)
          .set(domain.Notification.column.is_read -> true)
          .where
          .eq(domain.Notification.column.user_id, userId)
      }.update()

      if (count < 1)
        Failure(NDLASQLException(s"Updating a notification with user_id '$userId' resulted in no affected row"))
      else Success(())
    }.flatten

    def readNotification(notificationId: Long, userId: Long)(implicit session: DBSession): Try[Unit] = Try {
      val count = withSQL {
        update(domain.Notification)
          .set(domain.Notification.column.is_read -> true)
          .where
          .eq(domain.Notification.column.id, notificationId)
          .and
          .eq(domain.Notification.column.user_id, userId)
      }.update()

      if (count < 1)
        Failure(
          NDLASQLException(
            s"Updating a notification with user_id '$userId' and notification_id '$notificationId' resulted in no affected row"
          )
        )
      else Success(())
    }.flatten

    def deleteNotification(notificationId: Long, userId: Long)(implicit session: DBSession): Try[Unit] = Try {
      val count = withSQL {
        delete
          .from(domain.Notification)
          .where
          .eq(domain.Notification.column.id, notificationId)
          .and
          .eq(domain.Notification.column.user_id, userId)
      }.update()

      if (count < 1)
        Failure(
          NDLASQLException(
            s"Deleting a notification with user_id '$userId' and notification_id '$notificationId' resulted in no affected row"
          )
        )
      else Success(())
    }.flatten

    def deleteNotifications(userId: Long)(implicit session: DBSession): Try[Unit] = Try {
      val count = withSQL {
        delete
          .from(domain.Notification)
          .where
          .eq(domain.Notification.column.user_id, userId)
      }.update()

      if (count < 1)
        Failure(
          NDLASQLException(
            s"Deleting a notification with user_id '$userId' resulted in no affected row"
          )
        )
      else Success(())
    }.flatten

    def followTopic(topicId: Long, userId: Long)(implicit session: DBSession): Try[domain.TopicFollow] = Try {
      val column = domain.TopicFollow.column.c _
      val inserted = withSQL {
        insert
          .into(domain.TopicFollow)
          .namedValues(
            column("user_id")  -> userId,
            column("topic_id") -> topicId
          )
      }.updateAndReturnGeneratedKey.apply()

      domain.TopicFollow(
        id = inserted,
        user_id = userId,
        topic_id = topicId
      )
    }

    def unfollowTopic(topicId: Long, userId: Long)(implicit session: DBSession): Try[Int] = Try {
      val tf = domain.TopicFollow.syntax("tf")
      val count = withSQL {
        delete
          .from(domain.TopicFollow as tf)
          .where
          .eq(tf.user_id, userId)
          .and
          .eq(tf.topic_id, topicId)
      }.update()
      if (count < 1)
        Failure(
          NDLASQLException(
            s"Deleting a topicfollow with user_id '$userId' and topic_id $topicId resulted in no affected row"
          )
        )
      else Success(count)
    }.flatten

    def getTopicFollowers(topicId: Long)(implicit session: DBSession): Try[List[MyNDLAUser]] = Try {
      val tf = domain.TopicFollow.syntax("tf")
      val u  = DBMyNDLAUser.syntax("u")
      sql"""
           select ${tf.resultAll}, ${u.resultAll}
           from ${domain.TopicFollow.as(tf)}
           left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${tf.user_id}
           where ${tf.topic_id} = $topicId
         """
        .map(rs => DBMyNDLAUser.fromResultSet(u)(rs))
        .list
        .apply()
    }

    def getTopicFollowing(topicId: Long, userId: Long)(implicit session: DBSession): Try[Option[domain.TopicFollow]] = {
      val tf = domain.TopicFollow.syntax("tf")
      Try {
        sql"""
             select ${tf.resultAll}
             from ${domain.TopicFollow.as(tf)}
             where ${tf.topic_id} = $topicId and ${tf.user_id} = $userId
           """
          .map(rs => domain.TopicFollow.fromResultSet(tf)(rs))
          .single
          .apply()
          .sequence
      }.flatten
    }

    def getFlagsForPost(postId: Long)(implicit session: DBSession): Try[List[CompiledFlag]] = Try {
      val f = domain.Flag.syntax("f")
      val u = DBMyNDLAUser.syntax("u")
      sql"""
           select ${f.resultAll}, ${u.resultAll}
           from ${domain.Flag.as(f)}
           left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${f.user_id}
           where ${f.post_id} = $postId
         """
        .one(rs => domain.Flag.fromResultSet(f.resultName)(rs))
        .toOne(rs => DBMyNDLAUser.fromResultSet(u)(rs))
        .map { (flag, user) => flag.map(CompiledFlag(_, user)) }
        .list
        .apply()
        .sequence
    }.flatten

    def resolveFlag(flagId: Long, resolveTime: NDLADate)(implicit session: DBSession): Try[Unit] = Try {
      val count = withSQL {
        update(domain.Flag)
          .set(domain.Flag.column.resolved -> resolveTime)
          .where
          .eq(domain.Flag.column.id, flagId)
      }.update()

      if (count < 1) Failure(NDLASQLException(s"Resolving a flag with id '$flagId' resulted in no affected row"))
      else Success(())
    }.flatten

    def getFlag(flagId: Long)(implicit session: DBSession) = {
      val f = domain.Flag.syntax("f")
      Try {
        sql"""
                 select ${f.resultAll}
                 from ${domain.Flag.as(f)}
                 where ${f.id} = $flagId
                 """
          .map(rs => domain.Flag.fromResultSet(f.resultName)(rs))
          .single
          .apply()
          .sequence
      }.flatten
    }

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

    def getPost(postId: Long)(implicit session: DBSession): Try[Option[(domain.Post, MyNDLAUser)]] = {
      val p = domain.Post.syntax("p")
      val u = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
                 select ${p.resultAll}, ${u.resultAll}
                 from ${domain.Post.as(p)}
                 left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${p.ownerId}
                 where ${p.id} = $postId
             """
          .one(rs => domain.Post.fromResultSet(p.resultName)(rs))
          .toOne(rs => DBMyNDLAUser.fromResultSet(u)(rs))
          .map { (post, user) => post.map(_ -> user) }
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
      val created = clock.now()
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

    def getTopic(topicId: Long)(implicit session: DBSession): Try[Option[CompiledTopic]] = {
      val t = domain.Topic.syntax("t")
      val u = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
             select ${t.resultAll}, ${u.resultAll}
             from ${domain.Topic.as(t)}
             left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${t.ownerId}
             where ${t.id} = $topicId
           """
          .one(rs => domain.Topic.fromResultSet(t.resultName)(rs))
          .toMany(rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption)
          .map { (topic, owner) => compileTopic(topic, owner.toList) }
          .single
          .apply()
          .sequence
      }.flatten
    }

    def compileTopic(topic: Try[domain.Topic], owners: Seq[MyNDLAUser]): Try[CompiledTopic] = {
      for {
        t <- topic
        owner <- owners
          .find(_.id == t.ownerId)
          .toTry(NDLASQLException(s"Topic with id ${t.id} has no owner in result."))
      } yield CompiledTopic(
        topic = t,
        owner = owner
      )
    }

    def compilePost(
        post: domain.Post,
        owners: Seq[MyNDLAUser],
        flags: Seq[domain.Flag]
    ): Try[CompiledPost] = for {
      postOwner <- owners
        .find(_.id == post.ownerId)
        .toTry(NDLASQLException(s"Post with id ${post.id} has no owner in result."))
      postFlags = flags.filter(f => f.post_id == post.id)
      flagsWithFlaggers <- postFlags.traverse(f => {
        owners
          .find(_.id == f.user_id)
          .toTry(NDLASQLException(s"Flag with id ${f.id} has no flagger in result."))
          .map(flagger => {
            CompiledFlag(flag = f, flagger = flagger)
          })
      })
    } yield CompiledPost(post = post, owner = postOwner, flagsWithFlaggers.toList)

    def topicCountWhere(extra: SQLSyntax)(implicit session: DBSession): Try[Long] = Try {
      sql"""
           select count(*) as count
           from ${domain.Topic.table}
           $extra
         """
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0L)
    }

    def postCount(topicId: Long)(implicit session: DBSession): Try[Long] = Try {
      val p = domain.Post.syntax("p")
      sql"""
           select count(*) as count
           from ${domain.Post.as(p)}
           where ${p.topic_id} = $topicId
         """
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0L)
    }

    def notificationsCount(userId: Long)(implicit session: DBSession): Try[Long] = Try {
      val n = domain.Notification.syntax("n")
      sql"""
           select count(*) as count
           from ${domain.Notification.as(n)}
           where ${n.user_id} = $userId
         """
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0L)
    }

    def getTopicsPaginated(offset: Long, limit: Long)(implicit session: DBSession): Try[(List[CompiledTopic], Long)] = {
      for {
        topics <- getTopicsPaginatedWhere(sqls"", offset, limit)
        count  <- topicCountWhere(sqls"")
      } yield (topics, count)
    }

    def getFlaggedPostsCount(implicit session: DBSession): Try[Long] = Try {
      sql"""
           select count(*) as count
           from ${domain.Flag.table}
           where ${domain.Flag.column.resolved} is null
         """
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0L)
    }

    def getFlaggedPosts(offset: Long, limit: Long)(implicit session: DBSession): Try[List[CompiledPost]] = {
      val p  = domain.Post.syntax("p")
      val ps = SubQuery.syntax("ps").include(p)
      val u  = DBMyNDLAUser.syntax("u")
      val f  = domain.Flag.syntax("f")
      Try {
        sql"""
              select ${ps.resultAll}, ${u.resultAll}, ${f.resultAll}
              from (
                  select ${p.resultAll}
                  from ${domain.Post.as(p)}
                  where (select count(*) from flags f where f.post_id = ${p.id} and f.resolved is null) > 0
                  order by ${p.created} asc nulls last, ${p.id} asc
                  limit $limit
                  offset $offset
                ) ps
               left join ${domain.Flag.as(f)} on ${f.post_id} = ${ps(p).id} and ${f.resolved} is null
               left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${ps(p).ownerId} OR ${u.id} = ${f.user_id}
               order by ${ps(p).created} asc nulls last
           """
          .one(rs => domain.Post.fromResultSet(ps(p).resultName)(rs))
          .toManies(
            rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption,
            rs => domain.Flag.fromResultSet(f)(rs).toOption
          )
          .map((posts, owners, flags) => posts.flatMap(pp => compilePost(pp, owners.toList, flags.toList)))
          .list
          .apply()
          .sequence
      }.flatten
    }

    def getUserTopicsPaginated(userId: Long, offset: Long, limit: Long)(implicit
        session: DBSession
    ): Try[(List[CompiledTopic], Long)] = {
      for {
        topics <- getTopicsPaginatedWhere(sqls"where owner_id = $userId", offset, limit)
        count  <- topicCountWhere(sqls"where owner_id = $userId")
      } yield (topics, count)
    }

    def getTopicsPaginatedWhere(where: SQLSyntax, offset: Long, limit: Long)(implicit
        session: DBSession
    ): Try[List[CompiledTopic]] = {
      val t  = domain.Topic.syntax("t")
      val ts = SubQuery.syntax("ts").include(t)
      val u  = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
              select ${ts.resultAll}, ${u.resultAll}
              from (
                  select ${t.resultAll}, (select max(pp.created) from posts pp where pp.topic_id = ${t.id}) as newest_post_date
                  from ${domain.Topic.as(t)}
                  $where
                  order by newest_post_date desc nulls last
                  limit $limit
                  offset $offset
                ) ts
               left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${ts(t).ownerId}
               order by newest_post_date desc nulls last
           """
          .one(rs => domain.Topic.fromResultSet(ts(t).resultName)(rs))
          .toMany(rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption)
          .map { (topic, owners) => compileTopic(topic, owners.toList) }
          .list
          .apply()
          .sequence
      }.flatten
    }

    def getPostsForTopic(topicId: Long, offset: Long, limit: Long)(implicit
        session: DBSession
    ): Try[List[CompiledPost]] = {
      val p  = domain.Post.syntax("p")
      val ps = SubQuery.syntax("ps").include(p)
      val u  = DBMyNDLAUser.syntax("u")
      val f  = domain.Flag.syntax("f")
      Try {
        sql"""
              select ${ps.resultAll}, ${u.resultAll}, ${f.resultAll}
              from (
                  select ${p.resultAll}
                  from ${domain.Post.as(p)}
                  where ${p.topic_id} = $topicId
                  order by ${p.created} asc nulls last, ${p.id} asc
                  limit $limit
                  offset $offset
                ) ps
               left join ${domain.Flag.as(f)} on ${f.post_id} = ${ps(p).id} and ${f.resolved} is null
               left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${ps(p).ownerId} OR ${u.id} = ${f.user_id}
               order by ${ps(p).created} asc nulls last
           """
          .one(rs => domain.Post.fromResultSet(ps(p).resultName)(rs))
          .toManies(
            rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption,
            rs => domain.Flag.fromResultSet(f)(rs).toOption
          )
          .map((posts, owners, flags) => posts.flatMap(pp => compilePost(pp, owners.toList, flags.toList)))
          .list
          .apply()
          .sequence
      }.flatten
    }

    def getTopicsForCategory(categoryId: Long, offset: Long, limit: Long)(implicit
        session: DBSession
    ): Try[List[CompiledTopic]] = {
      val t  = domain.Topic.syntax("t")
      val ts = SubQuery.syntax("ts").include(t)
      val u  = DBMyNDLAUser.syntax("u")
      Try {
        sql"""
              select ${ts.resultAll}, ${u.resultAll}
              from (
                  select ${t.resultAll}, (select max(pp.created) from posts pp where pp.topic_id = ${t.id}) as newest_post_date
                  from ${domain.Topic.as(t)}
                  where ${t.category_id} = $categoryId
                  order by newest_post_date desc nulls last
                  limit $limit
                  offset $offset
                ) ts
               left join ${DBMyNDLAUser.as(u)} on ${u.id} = ${ts(t).ownerId}
               order by newest_post_date desc nulls last
           """
          .one(rs => domain.Topic.fromResultSet(ts(t).resultName)(rs))
          .toMany(rs => Try(DBMyNDLAUser.fromResultSet(u)(rs)).toOption)
          .map { (topic, owner) => compileTopic(topic, owner.toList) }
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

    def deleteAllPosts(implicit session: DBSession): Try[Unit] = Try {
      val numRows = sql"delete from ${domain.Post.table}".update()
      logger.info(s"Deleted $numRows posts")
    }

    def deleteAllTopics(implicit session: DBSession): Try[Unit] = Try {
      val numRows = sql"delete from ${domain.Topic.table}".update()
      logger.info(s"Deleted $numRows topics")
    }

    def deleteAllCategories(implicit session: DBSession): Try[Unit] = Try {
      val numRows = sql"delete from ${domain.Category.table}".update()
      logger.info(s"Deleted $numRows categories")
    }

    def resetSequences(implicit session: DBSession): Try[Unit] = Try {
      sql"""
           alter sequence categories_id_seq restart with 1;
           alter sequence topics_id_seq restart with 1;
           alter sequence posts_id_seq restart with 1;
           alter sequence flags_id_seq restart with 1;
         """.execute(): Unit
    }
  }
}
