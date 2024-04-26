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
case class UserFolder(
    @description("The users own folders") folders: List[Folder],
    @description("The shared folder the user has saved") sharedFolders: List[Folder]
)
