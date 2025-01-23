// DO NOT EDIT: generated file by scala-tsi

export type ArenaGroup = "ADMIN"

export interface IArenaUserDTO {
  id: number
  displayName: string
  username: string
  location: string
  groups: ArenaGroup[]
}

export interface IBreadcrumbDTO {
  id: string
  name: string
}

export interface ICategoryBreadcrumbDTO {
  id: number
  title: string
}

export interface ICategoryDTO {
  id: number
  title: string
  description: string
  topicCount: number
  postCount: number
  isFollowing: boolean
  visible: boolean
  rank: number
  parentCategoryId?: number
  categoryCount: number
  subcategories: ICategoryDTO[]
  breadcrumbs: ICategoryBreadcrumbDTO[]
}

export interface ICategoryWithTopicsDTO {
  id: number
  title: string
  description: string
  topicCount: number
  postCount: number
  topicPage: number
  topicPageSize: number
  topics: ITopicDTO[]
  isFollowing: boolean
  visible: boolean
  rank: number
  parentCategoryId?: number
  categoryCount: number
  subcategories: ICategoryDTO[]
  breadcrumbs: ICategoryBreadcrumbDTO[]
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

export interface IFlagDTO {
  id: number
  reason: string
  created: string
  resolved?: string
  isResolved: boolean
  flagger?: IArenaUserDTO
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
  role: string
  organization: string
  groups: IMyNDLAGroupDTO[]
  arenaEnabled: boolean
  arenaAccepted: boolean
  shareName: boolean
  arenaGroups: ArenaGroup[]
}

export interface INewCategoryDTO {
  title: string
  description: string
  visible: boolean
  parentCategoryId?: number
}

export interface INewFlagDTO {
  reason: string
}

export interface INewFolderDTO {
  name: string
  parentId?: string
  status?: string
  description?: string
}

export interface INewPostDTO {
  content: string
  toPostId?: number
}

export interface INewPostNotificationDTO {
  id: number
  topicId: number
  isRead: boolean
  topicTitle: string
  post: IPostDTO
  notificationTime: string
}

export interface INewResourceDTO {
  resourceType: ResourceType
  path: string
  tags?: string[]
  resourceId: string
}

export interface INewTopicDTO {
  title: string
  initialPost: INewPostDTO
  isLocked?: boolean
  isPinned?: boolean
}

export interface IOwnerDTO {
  name: string
}

export interface IPaginatedArenaUsersDTO {
  totalCount: number
  page: number
  pageSize: number
  items: IArenaUserDTO[]
}

export interface IPaginatedNewPostNotificationsDTO {
  totalCount: number
  page: number
  pageSize: number
  items: INewPostNotificationDTO[]
}

export interface IPaginatedPostsDTO {
  totalCount: number
  page: number
  pageSize: number
  items: IPostDTO[]
}

export interface IPaginatedTopicsDTO {
  totalCount: number
  page: number
  pageSize: number
  items: ITopicDTO[]
}

export interface IPostDTO {
  id: number
  content: string
  created: string
  updated: string
  owner?: IArenaUserDTO
  flags?: IFlagDTO[]
  topicId: number
  replies: IPostWrapperDTO[]
  upvotes: number
  upvoted: boolean
}

export type IPostWrapperDTO = IPostDTO

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
  favouritedResources: IResourceStatsDTO[]
  favourited: { [ key: string ]: number }
}

export interface ITopicDTO {
  id: number
  title: string
  postCount: number
  created: string
  updated: string
  categoryId: number
  isFollowing: boolean
  isLocked: boolean
  isPinned: boolean
  voteCount: number
}

export interface ITopicWithPostsDTO {
  id: number
  title: string
  postCount: number
  posts: IPaginatedPostsDTO
  created: string
  updated: string
  categoryId: number
  isFollowing: boolean
  isLocked: boolean
  isPinned: boolean
  voteCount: number
}

export interface IUpdatedFolderDTO {
  name?: string
  status?: string
  description?: string
}

export interface IUpdatedMyNDLAUserDTO {
  favoriteSubjects?: string[]
  arenaEnabled?: boolean
  shareName?: boolean
  arenaGroups?: ArenaGroup[]
  arenaAccepted?: boolean
}

export interface IUpdatedResourceDTO {
  tags?: string[]
  resourceId?: string
}

export interface IUserFolderDTO {
  folders: IFolderDTO[]
  sharedFolders: IFolderDTO[]
}

export type ResourceType = ("article" | "audio" | "concept" | "image" | "learningpath" | "multidisciplinary" | "topic" | "video")
