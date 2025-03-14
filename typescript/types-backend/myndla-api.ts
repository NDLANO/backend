// DO NOT EDIT: generated file by scala-tsi

export interface IBreadcrumbDTO {
  id: string
  name: string
}

export interface IConfigMetaDTO {
  key: string
  value: (boolean | string[])
  updatedAt: string
  updatedBy: string
}

export interface IConfigMetaRestrictedDTO {
  key: string
  value: (boolean | string[])
}

export interface IFolderDTO {
  id: string
  name: string
  status: string
  parentId?: string
  breadcrumbs: IBreadcrumbDTO[]
  subfolders: IFolderDataDTO[]
  resources: IResourceDTO[]
  rank: number
  created: string
  updated: string
  shared?: string
  description?: string
  owner?: IOwnerDTO
}

export type IFolderDataDTO = IFolderDTO

export interface IMyNDLAGroupDTO {
  id: string
  displayName: string
  isPrimarySchool: boolean
  parentId?: string
}

export interface IMyNDLAUserDTO {
  id: number
  feideId: string
  username: string
  email: string
  displayName: string
  favoriteSubjects: string[]
  role: UserRole
  organization: string
  groups: IMyNDLAGroupDTO[]
  arenaEnabled: boolean
  arenaAccepted: boolean
  shareNameAccepted: boolean
}

export interface INewFolderDTO {
  name: string
  parentId?: string
  status?: string
  description?: string
}

export interface INewResourceDTO {
  resourceType: ResourceType
  path: string
  tags?: string[]
  resourceId: string
}

export interface IOwnerDTO {
  name: string
}

export interface IResourceDTO {
  id: string
  resourceType: ResourceType
  path: string
  created: string
  tags: string[]
  resourceId: string
  rank?: number
}

export interface IResourceStatsDTO {
  type: string
  number: number
}

export interface ISingleResourceStatsDTO {
  id: string
  favourites: number
}

export interface IStatsDTO {
  numberOfUsers: number
  numberOfFolders: number
  numberOfResources: number
  numberOfTags: number
  numberOfSubjects: number
  numberOfSharedFolders: number
  numberOfMyNdlaLearningPaths: number
  favouritedResources: IResourceStatsDTO[]
  favourited: { [ key: string ]: number }
  users: IUserStatsDTO
}

export interface IUpdatedFolderDTO {
  name?: string
  status?: string
  description?: string
}

export interface IUpdatedMyNDLAUserDTO {
  favoriteSubjects?: string[]
  arenaEnabled?: boolean
  arenaAccepted?: boolean
  shareNameAccepted?: boolean
}

export interface IUpdatedResourceDTO {
  tags?: string[]
  resourceId?: string
}

export interface IUserFolderDTO {
  folders: IFolderDTO[]
  sharedFolders: IFolderDTO[]
}

export interface IUserStatsDTO {
  total: number
  employees: number
  students: number
  withFavourites: number
  noFavourites: number
  arena: number
}

export type ResourceType = ("article" | "audio" | "concept" | "image" | "learningpath" | "multidisciplinary" | "topic" | "video")

export type UserRole = ("employee" | "student")
