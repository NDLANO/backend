// DO NOT EDIT: generated file by scala-tsi

export type Availability = ("everyone" | "teacher")

export type ContributorType = ("artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer")

export enum DraftSortEnum {
  ByRelevanceDesc = "-relevance",
  ByRelevanceAsc = "relevance",
  ByTitleDesc = "-title",
  ByTitleAsc = "title",
  ByLastUpdatedDesc = "-lastUpdated",
  ByLastUpdatedAsc = "lastUpdated",
  ByIdDesc = "-id",
  ByIdAsc = "id",
}

export type Grade = (1 | 2 | 3 | 4 | 5)

export interface IArticleContentDTO {
  content: string
  language: string
}

export interface IArticleDTO {
  id: number
  oldNdlaUrl?: string
  revision: number
  status: IStatusDTO
  title?: IArticleTitleDTO
  content?: IArticleContentDTO
  copyright?: IDraftCopyrightDTO
  tags?: IArticleTagDTO
  requiredLibraries: IRequiredLibraryDTO[]
  visualElement?: IVisualElementDTO
  introduction?: IArticleIntroductionDTO
  metaDescription?: IArticleMetaDescriptionDTO
  metaImage?: IArticleMetaImageDTO
  created: string
  updated: string
  updatedBy: string
  published: string
  articleType: string
  supportedLanguages: string[]
  notes: IEditorNoteDTO[]
  editorLabels: string[]
  grepCodes: string[]
  conceptIds: number[]
  availability: string
  relatedContent: (IRelatedContentLinkDTO | number)[]
  revisions: IRevisionMetaDTO[]
  responsible?: IDraftResponsibleDTO
  slug?: string
  comments: ICommentDTO[]
  prioritized: boolean
  priority: string
  started: boolean
  qualityEvaluation?: IQualityEvaluationDTO
  disclaimer?: IDisclaimerDTO
}

export interface IArticleIntroductionDTO {
  introduction: string
  htmlIntroduction: string
  language: string
}

export interface IArticleMetaDescriptionDTO {
  metaDescription: string
  language: string
}

export interface IArticleMetaImageDTO {
  url: string
  alt: string
  language: string
}

export interface IArticleSearchParamsDTO {
  query?: string
  language?: string
  license?: string
  page?: number
  pageSize?: number
  ids?: number[]
  articleTypes?: string[]
  sort?: Sort
  scrollId?: string
  fallback?: boolean
  grepCodes?: string[]
}

export interface IArticleSummaryDTO {
  id: number
  title: IArticleTitleDTO
  visualElement?: IVisualElementDTO
  introduction?: IArticleIntroductionDTO
  url: string
  license: string
  articleType: string
  supportedLanguages: string[]
  tags?: IArticleTagDTO
  notes: string[]
  users: string[]
  grepCodes: string[]
  status: IStatusDTO
  updated: string
}

export interface IArticleTagDTO {
  tags: string[]
  language: string
}

export interface IArticleTitleDTO {
  title: string
  htmlTitle: string
  language: string
}

export interface IAuthorDTO {
  type: ContributorType
  name: string
}

export interface ICommentDTO {
  id: string
  content: string
  created: string
  updated: string
  isOpen: boolean
  solved: boolean
}

export interface IDisclaimerDTO {
  disclaimer: string
  language: string
}

export interface IDraftCopyrightDTO {
  license?: ILicenseDTO
  origin?: string
  creators: IAuthorDTO[]
  processors: IAuthorDTO[]
  rightsholders: IAuthorDTO[]
  validFrom?: string
  validTo?: string
  processed: boolean
}

export interface IDraftResponsibleDTO {
  responsibleId: string
  lastUpdated: string
}

export interface IEditorNoteDTO {
  note: string
  user: string
  status: IStatusDTO
  timestamp: string
}

export interface IGrepCodesSearchResultDTO {
  totalCount: number
  page: number
  pageSize: number
  results: string[]
}

export interface ILicenseDTO {
  license: string
  description?: string
  url?: string
}

export interface INewArticleDTO {
  language: string
  title: string
  published?: string
  content?: string
  tags?: string[]
  introduction?: string
  metaDescription?: string
  metaImage?: INewArticleMetaImageDTO
  visualElement?: string
  copyright?: IDraftCopyrightDTO
  requiredLibraries?: IRequiredLibraryDTO[]
  articleType: string
  notes?: string[]
  editorLabels?: string[]
  grepCodes?: string[]
  conceptIds?: number[]
  availability?: string
  relatedContent?: (IRelatedContentLinkDTO | number)[]
  revisionMeta?: IRevisionMetaDTO[]
  responsibleId?: string
  slug?: string
  comments?: INewCommentDTO[]
  prioritized?: boolean
  priority?: string
  qualityEvaluation?: IQualityEvaluationDTO
  disclaimer?: string
}

export interface INewArticleMetaImageDTO {
  id: string
  alt: string
}

export interface INewCommentDTO {
  content: string
  isOpen?: boolean
}

export interface IQualityEvaluationDTO {
  grade: Grade
  note?: string
}

export interface IRelatedContentLinkDTO {
  title: string
  url: string
}

export interface IRequiredLibraryDTO {
  mediaType: string
  name: string
  url: string
}

export interface IRevisionMetaDTO {
  id?: string
  revisionDate: string
  note: string
  status: string
}

export interface ISavedSearchDTO {
  searchUrl: string
  searchPhrase: string
}

export interface ISearchResultDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: IArticleSummaryDTO[]
}

export interface IStatusDTO {
  current: string
  other: string[]
}

export interface ITagsSearchResultDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface IUpdatedArticleDTO {
  revision: number
  language?: string
  title?: string
  status?: string
  published?: string
  content?: string
  tags?: string[]
  introduction?: string
  metaDescription?: string
  metaImage: UpdateOrDeleteNewArticleMetaImageDTO
  visualElement?: string
  copyright?: IDraftCopyrightDTO
  requiredLibraries?: IRequiredLibraryDTO[]
  articleType?: string
  notes?: string[]
  editorLabels?: string[]
  grepCodes?: string[]
  conceptIds?: number[]
  createNewVersion?: boolean
  availability?: string
  relatedContent?: (IRelatedContentLinkDTO | number)[]
  revisionMeta?: IRevisionMetaDTO[]
  responsibleId: UpdateOrDeleteString
  slug?: string
  comments?: IUpdatedCommentDTO[]
  prioritized?: boolean
  priority?: string
  qualityEvaluation?: IQualityEvaluationDTO
  disclaimer?: string
}

export interface IUpdatedCommentDTO {
  id?: string
  content: string
  isOpen?: boolean
  solved?: boolean
}

export interface IUpdatedUserDataDTO {
  savedSearches?: ISavedSearchDTO[]
  latestEditedArticles?: string[]
  latestEditedConcepts?: string[]
  favoriteSubjects?: string[]
}

export interface IUploadedFileDTO {
  filename: string
  mime: string
  extension: string
  path: string
}

export interface IUserDataDTO {
  userId: string
  savedSearches?: ISavedSearchDTO[]
  latestEditedArticles?: string[]
  latestEditedConcepts?: string[]
  favoriteSubjects?: string[]
}

export interface IVisualElementDTO {
  visualElement: string
  language: string
}

export type Sort = DraftSortEnum

export type UpdateOrDeleteNewArticleMetaImageDTO = (null | undefined | INewArticleMetaImageDTO)

export type UpdateOrDeleteString = (null | undefined | string)
