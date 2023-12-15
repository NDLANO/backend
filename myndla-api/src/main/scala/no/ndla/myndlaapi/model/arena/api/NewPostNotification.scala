/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

sealed trait Notifications {
  val id: Long
  val notificationTime: NDLADate
}

@description("Notification data")
case class NewPostNotification(
    @description("The notification id") id: Long,
    @description("The topic id which got a new post") topicId: Long,
    @description("The topic title which got a new post") topicTitle: String,
    @description("The new post id") post: Post,
    @description("Notification time") notificationTime: NDLADate
) extends Notifications
