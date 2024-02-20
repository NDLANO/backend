// DO NOT EDIT: generated file by scala-tsi

export type ArenaGroup = "ADMIN"

export interface IArticle {
  type: "Article"
}

export interface IAudio {
  type: "Audio"
}

export interface IAuthor {
  type: string
  name: string
}

export interface IBreadcrumb {
  id: string
  name: string
}

export interface IConcept {
  type: "Concept"
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

export interface ICopyright {
  license: ILicense
  contributors: IAuthor[]
}

export interface ICoverPhoto {
  url: string
  metaUrl: string
}

export interface IDescription {
  description: string
  language: string
}

export interface IEmbedUrlV2 {
  url: string
  embedType: string
}

export interface IError {
  code: string
  description: string
  occuredAt: string
}

export interface IFolder {
  id: string
  name: string
  status: string
  parentId?: string
  breadcrumbs: IBreadcrumb[]
  subfolders: IFolderData[]
  resources: IResource[]
  rank?: number
  created: string
  updated: string
  shared?: string
  description?: string
  owner?: IOwner
}

export type IFolderData = IFolder

export interface IImage {
  type: "Image"
}

export interface IIntroduction {
  introduction: string
  language: string
}

export interface ILearningPathStatus {
  status: string
}

export interface ILearningPathSummaryV2 {
  id: number
  revision?: number
  title: ITitle
  description: IDescription
  introduction: IIntroduction
  metaUrl: string
  coverPhotoUrl?: string
  duration?: number
  status: string
  lastUpdated: string
  tags: ILearningPathTags
  copyright: ICopyright
  supportedLanguages: string[]
  isBasedOn?: number
  message?: string
}

export interface ILearningPathTags {
  tags: string[]
  language: string
}

export interface ILearningPathTagsSummary {
  language: string
  supportedLanguages: string[]
  tags: string[]
}

export interface ILearningPathV2 {
  id: number
  revision: number
  isBasedOn?: number
  title: ITitle
  description: IDescription
  metaUrl: string
  learningsteps: ILearningStepV2[]
  learningstepUrl: string
  coverPhoto?: ICoverPhoto
  duration?: number
  status: string
  verificationStatus: string
  lastUpdated: string
  tags: ILearningPathTags
  copyright: ICopyright
  canEdit: boolean
  supportedLanguages: string[]
  ownerId?: string
  message?: IMessage
}

export interface ILearningStepContainerSummary {
  language: string
  learningsteps: ILearningStepSummaryV2[]
  supportedLanguages: string[]
}

export interface ILearningStepSeqNo {
  seqNo: number
}

export interface ILearningStepStatus {
  status: string
}

export interface ILearningStepSummaryV2 {
  id: number
  seqNo: number
  title: ITitle
  type: string
  metaUrl: string
}

export interface ILearningStepV2 {
  id: number
  revision: number
  seqNo: number
  title: ITitle
  description?: IDescription
  embedUrl?: IEmbedUrlV2
  showTitle: boolean
  type: string
  license?: ILicense
  metaUrl: string
  canEdit: boolean
  status: string
  supportedLanguages: string[]
}

export interface ILearningpath {
  type: "Learningpath"
}

export interface ILicense {
  license: string
  description?: string
  url?: string
}

export interface IMessage {
  message: string
  date: string
}

export interface IMultidisciplinary {
  type: "Multidisciplinary"
}

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

export interface INewFolder {
  name: string
  parentId?: string
  status?: string
  description?: string
}

export interface INewResource {
  resourceType: ResourceType
  path: string
  tags?: string[]
  resourceId: string
}

export interface IOwner {
  name: string
}

export interface IResource {
  id: string
  resourceType: ResourceType
  path: string
  created: string
  tags: string[]
  resourceId: string
  rank?: number
}

export interface ISearchResultV2 {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: ILearningPathSummaryV2[]
}

export interface ITitle {
  title: string
  language: string
}

export interface IUpdatedFolder {
  name?: string
  status?: string
  description?: string
}

export interface IUpdatedResource {
  tags?: string[]
  resourceId?: string
}

export interface IVideo {
  type: "Video"
}

export type ResourceType = (IAudio | ILearningpath | IVideo | IImage | IArticle | IMultidisciplinary | IConcept)

export type ResourceType = (IConcept | IVideo | IArticle | ILearningpath | IMultidisciplinary | IAudio | IImage)
