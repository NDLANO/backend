// DO NOT EDIT: generated file by scala-tsi

export type Availability = ("everyone" | "teacher")

export type Grade = (1 | 2 | 3 | 4 | 5)

export interface IArticle {
  id: number
  oldNdlaUrl?: string
  revision: number
  status: IStatus
  title?: IArticleTitle
  content?: IArticleContent
  copyright?: IDraftCopyright
  tags?: IArticleTag
  requiredLibraries: IRequiredLibrary[]
  visualElement?: IVisualElement
  introduction?: IArticleIntroduction
  metaDescription?: IArticleMetaDescription
  metaImage?: IArticleMetaImage
  created: string
  updated: string
  updatedBy: string
  published: string
  articleType: string
  supportedLanguages: string[]
  notes: IEditorNote[]
  editorLabels: string[]
  grepCodes: string[]
  conceptIds: number[]
  availability: string
  relatedContent: (IRelatedContentLink | number)[]
  revisions: IRevisionMeta[]
  responsible?: IDraftResponsible
  slug?: string
  comments: IComment[]
  prioritized: boolean
  priority: string
  started: boolean
  qualityEvaluation?: IQualityEvaluation
}

export interface IArticleContent {
  content: string
  language: string
}

export interface IArticleIntroduction {
  introduction: string
  htmlIntroduction: string
  language: string
}

export interface IArticleMetaDescription {
  metaDescription: string
  language: string
}

export interface IArticleMetaImage {
  url: string
  alt: string
  language: string
}

export interface IArticleSummary {
  id: number
  title: IArticleTitle
  visualElement?: IVisualElement
  introduction?: IArticleIntroduction
  url: string
  license: string
  articleType: string
  supportedLanguages: string[]
  tags?: IArticleTag
  notes: string[]
  users: string[]
  grepCodes: string[]
  status: IStatus
  updated: string
}

export interface IArticleTag {
  tags: string[]
  language: string
}

export interface IArticleTitle {
  title: string
  htmlTitle: string
  language: string
}

export interface IAuthor {
  type: string
  name: string
}

export interface IComment {
  id: string
  content: string
  created: string
  updated: string
  isOpen: boolean
  solved: boolean
}

export interface IDraftCopyright {
  license?: ILicense
  origin?: string
  creators: IAuthor[]
  processors: IAuthor[]
  rightsholders: IAuthor[]
  validFrom?: string
  validTo?: string
  processed: boolean
}

export interface IDraftResponsible {
  responsibleId: string
  lastUpdated: string
}

export interface IEditorNote {
  note: string
  user: string
  status: IStatus
  timestamp: string
}

export interface IGrepCodesSearchResult {
  totalCount: number
  page: number
  pageSize: number
  results: string[]
}

export interface ILicense {
  license: string
  description?: string
  url?: string
}

export interface INewArticle {
  language: string
  title: string
  published?: string
  content?: string
  tags?: string[]
  introduction?: string
  metaDescription?: string
  metaImage?: INewArticleMetaImage
  visualElement?: string
  copyright?: IDraftCopyright
  requiredLibraries?: IRequiredLibrary[]
  articleType: string
  notes?: string[]
  editorLabels?: string[]
  grepCodes?: string[]
  conceptIds?: number[]
  availability?: string
  relatedContent?: (IRelatedContentLink | number)[]
  revisionMeta?: IRevisionMeta[]
  responsibleId?: string
  slug?: string
  comments?: INewComment[]
  prioritized?: boolean
  priority?: string
  qualityEvaluation?: IQualityEvaluation
}

export interface INewArticleMetaImage {
  id: string
  alt: string
}

export interface INewComment {
  content: string
  isOpen?: boolean
}

export interface IQualityEvaluation {
  grade: Grade
  note?: string
}

export interface IRelatedContentLink {
  title: string
  url: string
}

export interface IRequiredLibrary {
  mediaType: string
  name: string
  url: string
}

export interface IRevisionMeta {
  id?: string
  revisionDate: string
  note: string
  status: string
}

export interface ISavedSearch {
  searchUrl: string
  searchPhrase: string
}

export interface ISearchResult {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: IArticleSummary[]
}

export interface IStatus {
  current: string
  other: string[]
}

export interface ITagsSearchResult {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface IUpdatedArticle {
  revision: number
  language?: string
  title?: string
  status?: string
  published?: string
  content?: string
  tags?: string[]
  introduction?: string
  metaDescription?: string
  metaImage: UpdateOrDeleteNewArticleMetaImage
  visualElement?: string
  copyright?: IDraftCopyright
  requiredLibraries?: IRequiredLibrary[]
  articleType?: string
  notes?: string[]
  editorLabels?: string[]
  grepCodes?: string[]
  conceptIds?: number[]
  createNewVersion?: boolean
  availability?: string
  relatedContent?: (IRelatedContentLink | number)[]
  revisionMeta?: IRevisionMeta[]
  responsibleId: UpdateOrDeleteString
  slug?: string
  comments?: IUpdatedComment[]
  prioritized?: boolean
  priority?: string
  qualityEvaluation?: IQualityEvaluation
}

export interface IUpdatedComment {
  id?: string
  content: string
  isOpen?: boolean
  solved?: boolean
}

export interface IUpdatedUserData {
  savedSearches?: ISavedSearch[]
  latestEditedArticles?: string[]
  latestEditedConcepts?: string[]
  favoriteSubjects?: string[]
}

export interface IUploadedFile {
  filename: string
  mime: string
  extension: string
  path: string
}

export interface IUserData {
  userId: string
  savedSearches?: ISavedSearch[]
  latestEditedArticles?: string[]
  latestEditedConcepts?: string[]
  favoriteSubjects?: string[]
}

export interface IVisualElement {
  visualElement: string
  language: string
}

export type UpdateOrDeleteNewArticleMetaImage = (null | undefined | INewArticleMetaImage)

export type UpdateOrDeleteString = (null | undefined | string)
