// DO NOT EDIT: generated file by scala-tsi

export type ArenaGroup = "ADMIN"

export interface IArenaUser {
  id: number
  displayName: string
  username: string
  location: string
  groups: ArenaGroup[]
}

export interface IBreadcrumb {
  id: string
  name: string
}

export interface ICategory {
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
  subcategories: ICategory[]
  breadcrumbs: ICategoryBreadcrumb[]
}

export interface ICategoryBreadcrumb {
  id: number
  title: string
}

export interface ICategoryWithTopics {
  id: number
  title: string
  description: string
  topicCount: number
  postCount: number
  topicPage: number
  topicPageSize: number
  topics: ITopic[]
  isFollowing: boolean
  visible: boolean
  rank: number
  parentCategoryId?: number
  categoryCount: number
  subcategories: ICategory[]
  breadcrumbs: ICategoryBreadcrumb[]
}

export interface IConfigMeta {
  key: string
  value: (boolean | string[])
  updatedAt: string
  updatedBy: string
}

export interface IConfigMetaRestricted {
  key: string
  value: (boolean | string[])
}

export interface IFlag {
  id: number
  reason: string
  created: string
  resolved?: string
  isResolved: boolean
  flagger?: IArenaUser
}

export interface IFolder {
  id: string
  name: string
  status: string
  parentId?: string
  breadcrumbs: IBreadcrumb[]
  subfolders: IFolderData[]
  resources: IResource[]
  rank: number
  created: string
  updated: string
  shared?: string
  description?: string
  owner?: IOwner
}

export type IFolderData = IFolder

export interface IMyNDLAGroup {
  id: string
  displayName: string
  isPrimarySchool: boolean
  parentId?: string
}

export interface IMyNDLAUser {
  id: number
  feideId: string
  username: string
  email: string
  displayName: string
  favoriteSubjects: string[]
  role: string
  organization: string
  groups: IMyNDLAGroup[]
  arenaEnabled: boolean
  shareName: boolean
  arenaGroups: ArenaGroup[]
}

export interface INewCategory {
  title: string
  description: string
  visible: boolean
  parentCategoryId?: number
}

export interface INewFlag {
  reason: string
}

export interface INewFolder {
  name: string
  parentId?: string
  status?: string
  description?: string
}

export interface INewPost {
  content: string
  toPostId?: number
}

export interface INewPostNotification {
  id: number
  topicId: number
  isRead: boolean
  topicTitle: string
  post: IPost
  notificationTime: string
}

export interface INewResource {
  resourceType: ResourceType
  path: string
  tags?: string[]
  resourceId: string
}

export interface INewTopic {
  title: string
  initialPost: INewPost
  isLocked?: boolean
  isPinned?: boolean
}

export interface IOwner {
  name: string
}

export interface IPaginatedArenaUsers {
  totalCount: number
  page: number
  pageSize: number
  items: IArenaUser[]
}

export interface IPaginatedNewPostNotifications {
  totalCount: number
  page: number
  pageSize: number
  items: INewPostNotification[]
}

export interface IPaginatedPosts {
  totalCount: number
  page: number
  pageSize: number
  items: IPost[]
}

export interface IPaginatedTopics {
  totalCount: number
  page: number
  pageSize: number
  items: ITopic[]
}

export interface IPost {
  id: number
  content: string
  created: string
  updated: string
  owner?: IArenaUser
  flags?: IFlag[]
  topicId: number
  replies: IPostWrapper[]
  upvotes: number
  upvoted: boolean
}

export type IPostWrapper = IPost

export interface IResource {
  id: string
  resourceType: ResourceType
  path: string
  created: string
  tags: string[]
  resourceId: string
  rank?: number
}

export interface IResourceStats {
  type: string
  number: number
}

export interface ISingleResourceStats {
  id: string
  favourites: number
}

export interface IStats {
  numberOfUsers: number
  numberOfFolders: number
  numberOfResources: number
  numberOfTags: number
  numberOfSubjects: number
  numberOfSharedFolders: number
  favouritedResources: IResourceStats[]
  favourited: { [ key: string ]: number }
}

export interface ITopic {
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

export interface ITopicWithPosts {
  id: number
  title: string
  postCount: number
  posts: IPaginatedPosts
  created: string
  updated: string
  categoryId: number
  isFollowing: boolean
  isLocked: boolean
  isPinned: boolean
  voteCount: number
}

export interface IUpdatedFolder {
  name?: string
  status?: string
  description?: string
}

export interface IUpdatedMyNDLAUser {
  favoriteSubjects?: string[]
  arenaEnabled?: boolean
  shareName?: boolean
  arenaGroups?: ArenaGroup[]
}

export interface IUpdatedResource {
  tags?: string[]
  resourceId?: string
}

export interface IUserFolder {
  folders: IFolder[]
  sharedFolders: IFolder[]
}

export type ResourceType = ("concept" | "image" | "audio" | "multidisciplinary" | "article" | "learningpath" | "video")
