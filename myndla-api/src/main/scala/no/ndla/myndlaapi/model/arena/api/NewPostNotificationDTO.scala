/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.api

import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

sealed trait NotificationsDTO {
  val id: Long
  val notificationTime: NDLADate
}

@description("Notification data")
case class NewPostNotificationDTO(
    @description("The notification id") id: Long,
    @description("The topic id which got a new post") topicId: Long,
    @description("Whether the notification has been read or not") isRead: Boolean,
    @description("The topic title which got a new post") topicTitle: String,
    @description("The new post id") post: PostDTO,
    @description("Notification time") notificationTime: NDLADate
) extends NotificationsDTO
