// DO NOT EDIT: generated file by scala-tsi

export interface IApiTaxonomyContext {
  publicId: string
  root: string
  rootId: string
  relevance: string
  relevanceId: string
  path: string
  breadcrumbs: string[]
  contextId: string
  contextType: string
  resourceTypes: ITaxonomyResourceType[]
  language: string
  isPrimary: boolean
  isActive: boolean
  url: string
}

export interface IArticleIntroduction {
  introduction: string
  language: string
}

export interface IArticleResult {
  id: number
  title: ITitle
  introduction?: IArticleIntroduction
  articleType: string
  supportedLanguages: string[]
}

export interface IArticleResults {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: IArticleResult[]
}

export interface IAudioResult {
  id: number
  title: ITitle
  url: string
  supportedLanguages: string[]
}

export interface IAudioResults {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: IAudioResult[]
}

export interface IComment {
  id: string
  content: string
  created: string
  updated: string
  isOpen: boolean
  solved: boolean
}

export interface IDraftResponsible {
  responsibleId: string
  lastUpdated: string
}

export interface IDraftSearchParams {
  page?: number
  pageSize?: number
  articleTypes?: string[]
  contextTypes?: string[]
  language?: string
  ids?: number[]
  resourceTypes?: string[]
  license?: string
  query?: string
  noteQuery?: string
  sort?: Sort
  fallback?: boolean
  subjects?: string[]
  languageFilter?: string[]
  relevance?: string[]
  scrollId?: string
  draftStatus?: string[]
  users?: string[]
  grepCodes?: string[]
  aggregatePaths?: string[]
  embedResource?: string[]
  embedId?: string
  includeOtherStatuses?: boolean
  revisionDateFrom?: string
  revisionDateTo?: string
  excludeRevisionLog?: boolean
  responsibleIds?: string[]
  filterInactive?: boolean
  prioritized?: boolean
  priority?: string[]
  topics?: string[]
  publishedDateFrom?: string
  publishedDateTo?: string
  resultTypes?: SearchType[]
}

export interface IGroupSearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IMultiSearchSummary[]
  suggestions: IMultiSearchSuggestion[]
  aggregations: IMultiSearchTermsAggregation[]
  resourceType: string
}

export interface IHighlightedField {
  field: string
  matches: string[]
}

export interface IImageAltText {
  altText: string
  language: string
}

export interface IImageResult {
  id: number
  title: ITitle
  altText: IImageAltText
  previewUrl: string
  metaUrl: string
  supportedLanguages: string[]
}

export interface IImageResults {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: IImageResult[]
}

export interface ILearningPathIntroduction {
  introduction: string
  language: string
}

export interface ILearningpathResult {
  id: number
  title: ITitle
  introduction: ILearningPathIntroduction
  supportedLanguages: string[]
}

export interface ILearningpathResults {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: ILearningpathResult[]
}

export interface IMetaDescription {
  metaDescription: string
  language: string
}

export interface IMetaImage {
  url: string
  alt: string
  language: string
}

export interface IMultiSearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IMultiSearchSummary[]
  suggestions: IMultiSearchSuggestion[]
  aggregations: IMultiSearchTermsAggregation[]
}

export interface IMultiSearchSuggestion {
  name: string
  suggestions: ISearchSuggestion[]
}

export interface IMultiSearchSummary {
  id: number
  title: ITitle
  metaDescription: IMetaDescription
  metaImage?: IMetaImage
  url: string
  contexts: IApiTaxonomyContext[]
  supportedLanguages: string[]
  learningResourceType: LearningResourceType
  status?: IStatus
  traits: string[]
  score: number
  highlights: IHighlightedField[]
  paths: string[]
  lastUpdated: string
  license?: string
  revisions: IRevisionMeta[]
  responsible?: IDraftResponsible
  comments?: IComment[]
  prioritized?: boolean
  priority?: string
  resourceTypeName?: string
  parentTopicName?: string
  primaryRootName?: string
  published?: string
  favorited?: number
  resultType: SearchType
  conceptSubjectIds?: string[]
}

export interface IMultiSearchTermsAggregation {
  field: string
  sumOtherDocCount: number
  docCountErrorUpperBound: number
  values: ITermValue[]
}

export interface IRevisionMeta {
  revisionDate: string
  note: string
  status: string
}

export interface ISearchError {
  type: string
  errorMsg: string
}

export interface ISearchSuggestion {
  text: string
  offset: number
  length: number
  options: ISuggestOption[]
}

export interface IStatus {
  current: string
  other: string[]
}

export interface ISubjectAggregation {
  subjectId: string
  publishedArticleCount: number
  oldArticleCount: number
  revisionCount: number
  flowCount: number
  favoritedCount: number
}

export interface ISubjectAggregations {
  subjects: ISubjectAggregation[]
}

export interface ISubjectAggsInput {
  subjects?: string[]
}

export interface ISuggestOption {
  text: string
  score: number
}

export interface ITaxonomyResourceType {
  id: string
  name: string
  language: string
}

export interface ITermValue {
  value: string
  count: number
}

export interface ITitle {
  title: string
  language: string
}

export type LearningResourceType = ("standard" | "topic-article" | "frontpage-article" | "learningpath" | "concept" | "gloss")

export type SearchType = ("article" | "draft" | "learningpath" | "concept")

export type Sort = SortEnum

export enum SortEnum {
  ByRelevanceDesc = "-relevance",
  ByRelevanceAsc = "relevance",
  ByTitleDesc = "-title",
  ByTitleAsc = "title",
  ByLastUpdatedDesc = "-lastUpdated",
  ByLastUpdatedAsc = "lastUpdated",
  ByIdDesc = "-id",
  ByIdAsc = "id",
  ByDurationDesc = "-duration",
  ByDurationAsc = "duration",
  ByRevisionDateAsc = "revisionDate",
  ByRevisionDateDesc = "-revisionDate",
  ByResponsibleLastUpdatedAsc = "responsibleLastUpdated",
  ByResponsibleLastUpdatedDesc = "-responsibleLastUpdated",
  ByStatusAsc = "status",
  ByStatusDesc = "-status",
  ByPrioritizedDesc = "-prioritized",
  ByPrioritizedAsc = "prioritized",
  ByParentTopicNameDesc = "-parentTopicName",
  ByParentTopicNameAsc = "parentTopicName",
  ByPrimaryRootDesc = "-primaryRoot",
  ByPrimaryRootAsc = "primaryRoot",
  ByResourceTypeDesc = "-resourceType",
  ByResourceTypeAsc = "resourceType",
  ByPublishedDesc = "-published",
  ByPublishedAsc = "published",
  ByFavoritedDesc = "-favorited",
  ByFavoritedAsc = "favorited",
}
