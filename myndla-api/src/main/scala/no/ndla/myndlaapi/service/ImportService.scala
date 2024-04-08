/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndlaapi.integration.nodebb.{
  CategoryInList,
  ImportException,
  NodeBBClient,
  Owner,
  Post,
  SingleTopic,
  TopicInList
}
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import scalikejdbc._
import no.ndla.common.implicits.{OptionImplicit, TryQuestionMark}
import no.ndla.common.model.NDLADate
import no.ndla.myndlaapi.model.arena.domain.InsertCategory
import no.ndla.myndlaapi.model.arena.domain
import no.ndla.myndlaapi.model.domain.{ArenaGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.myndlaapi.repository.{ArenaRepository, UserRepository}
import scalikejdbc.DBSession

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait ImportService {
  this: ArenaReadService with NodeBBClient with ArenaRepository with UserRepository with Clock =>

  val importService: ImportService

  class ImportService extends StrictLogging {

    def importArenaDataFromNodeBB(): Try[Unit] =
      arenaRepository.rollbackOnFailure(session => importCategories(session))

    private def importCategories(session: DBSession): Try[Unit] = {
      for {
        adminUser        <- getOrCreateAdminUser(session)
        nodebbCategories <- nodebb.getCategories
        _                <- nodebbCategories.categories.traverse { c => importCategory(c, adminUser, None)(session) }
      } yield ()
    }

    private def getOrCreateAdminUser(session: DBSession): Try[MyNDLAUser] = {
      val existing = userRepository.userWithFeideId("ndla_admin")(session)
      existing.flatMap {
        case Some(admin) => Success(admin)
        case None =>
          userRepository.reserveFeideIdIfNotExists("ndla_admin")(session): Unit
          val toInsert = MyNDLAUserDocument(
            favoriteSubjects = Seq.empty,
            userRole = UserRole.EMPLOYEE,
            lastUpdated = clock.now(),
            organization = "NDLA",
            groups = Seq.empty,
            username = "ndla_admin",
            displayName = "ndla_admin",
            email = "ndla@knowit.no",
            arenaEnabled = true,
            arenaGroups = List(ArenaGroup.ADMIN),
            shareName = true
          )
          userRepository.insertUser("ndla_admin", toInsert)(session)
      }
    }

    private def importCategory(category: CategoryInList, adminUser: MyNDLAUser, parentId: Option[Long])(
        session: DBSession
    ): Try[Unit] = {
      logger.info(s"Importing category ${category.cid}")
      nodebb.getSingleCategory(category.cid).flatMap { nodebbCategory =>
        for {
          importedCategory <- arenaRepository.insertCategory(
            InsertCategory(
              nodebbCategory.name,
              nodebbCategory.description,
              visible = true,
              parentCategoryId = parentId
            )
          )(session)
          _ <- nodebbCategory.topics.traverse { t => importTopic(t, importedCategory, adminUser)(session) }
          _ <- category.children
            .traverse(childCid => importCategory(childCid, adminUser, importedCategory.id.some)(session))
        } yield ()
      }
    }

    private def convertTimestampToNDLADate(timestamp: Long): NDLADate =
      NDLADate.fromUnixTime(timestamp / 1000)

    private def getPostsForTopic(topic: SingleTopic): Try[List[Post]] = {
      @tailrec
      def _getPostsForTopic(topic: SingleTopic, posts: List[Post]): Try[List[Post]] = {
        if (topic.pagination.currentPage >= topic.pagination.pageCount) {
          Success(posts)
        } else {
          val nextPageNum = topic.pagination.currentPage + 1
          nodebb.getSingleTopic(topic.tid, nextPageNum) match {
            case Failure(ex)   => Failure(ex)
            case Success(next) => _getPostsForTopic(next, posts ++ next.posts)
          }
        }
      }

      _getPostsForTopic(topic, topic.posts)
    }

    private def lookupOwner(owner: Owner)(session: DBSession): Try[MyNDLAUser] = {
      val where = sqls"""REGEXP_REPLACE(document->>'username', '[@''"]', '-') = ${owner.username}"""
      userRepository
        .userWhere(where)(session)
        .flatMap(_.toTry(ImportException(s"No owner found $owner"))) match {
        case Failure(ex) =>
          Failure(ex)
        case Success(user) =>
          Success(user)
      }
    }

    private def getTopicOwner(
        topic: SingleTopic,
        allPosts: List[Post],
        adminUser: MyNDLAUser
    )(
        session: DBSession
    ): Try[MyNDLAUser] = {
      val owner = allPosts
        .map(_.user)
        .find(_.uid == topic.uid)
        .toTry(ImportException(s"No owner found for topic ${topic.tid} with uid ${topic.uid}"))
        .?
      val adminUsers = List("ndla_admin", "gunnar", "Gunnar", "Moderator")
      if (adminUsers.contains(owner.username)) {
        Success(adminUser)
      } else {
        lookupOwner(owner)(session)
      }
    }

    private def importTopic(topic: TopicInList, importedCategory: domain.Category, adminUser: MyNDLAUser)(
        session: DBSession
    ): Try[Unit] = {
      logger.info(s"Importing topic ${topic.tid}")
      if (topic.deleted == 1) {
        logger.info(s"Skipping deleted topic ${topic.tid}")
        return Success(())
      }

      for {
        singleTopic <- nodebb.getSingleTopic(topic.tid, 1)
        allPosts    <- getPostsForTopic(singleTopic)
        topicOwner  <- getTopicOwner(singleTopic, allPosts, adminUser)(session)
        top <- arenaRepository.insertTopic(
          categoryId = importedCategory.id,
          title = topic.title,
          ownerId = topicOwner.id,
          created = convertTimestampToNDLADate(singleTopic.timestamp),
          updated = convertTimestampToNDLADate(singleTopic.timestamp),
          locked = topic.locked == 1,
          pinned = topic.pinned == 1
        )(session)
        _ <- importPosts(allPosts, top, adminUser)(session)
      } yield ()
    }

    private def importPost(post: Post, topic: domain.Topic, adminUser: MyNDLAUser)(
        session: DBSession
    ): Try[Unit] = {
      logger.info(s"Importing post ${post.pid}")
      if (post.deleted == 1) {
        logger.info(s"Skipping deleted post ${post.pid}")
        return Success(())
      }
      val adminUsers = List("ndla_admin", "gunnar", "Gunnar")
      val owner = if (adminUsers.contains(post.user.username)) {
        adminUser
      } else {
        lookupOwner(post.user)(session).?
      }

      val singlePost = nodebb.getSinglePost(post.pid).?

      val created = convertTimestampToNDLADate(post.timestamp)
      val updated = if (post.edited == 0) created else convertTimestampToNDLADate(post.edited)
      arenaRepository.postPost(topic.id, singlePost.content, owner.id, created, updated)(session).map(_ => ())
    }

    private def importPosts(
        allPosts: List[Post],
        importedTopic: domain.Topic,
        adminUser: MyNDLAUser
    )(session: DBSession): Try[Unit] =
      allPosts.traverse { p => importPost(p, importedTopic, adminUser)(session) }.map(_ => ())
  }
}
