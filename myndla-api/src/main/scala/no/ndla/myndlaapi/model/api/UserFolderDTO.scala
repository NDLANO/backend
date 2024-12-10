/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import sttp.tapir.Schema.annotations.description

@description("User folder data")
case class UserFolderDTO(
    @description("The users own folders") folders: List[FolderDTO],
    @description("The shared folder the user has saved") sharedFolders: List[FolderDTO]
)
