package no.ndla.learningpathapi.model.domain

case class FolderAndDirectChildren(
    folder: Folder,
    childrenFolders: Seq[Folder],
    childrenResources: Seq[FolderResource]
)
