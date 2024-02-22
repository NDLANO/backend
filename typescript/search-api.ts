// DO NOT EDIT: generated file by scala-tsi

export interface IApiTaxonomyContext {
  id: string
  publicId: string
  subject: string
  root: string
  subjectId: string
  rootId: string
  relevance: string
  relevanceId: string
  path: string
  breadcrumbs: string[]
  learningResourceType: string
  contextType: string
  resourceTypes: ITaxonomyResourceType[]
  language: string
  isPrimaryConnection: boolean
  isPrimary: boolean
  isActive: boolean
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
  learningResourceIds?: number[]
  resourceTypes?: string[]
  license?: string
  query?: string
  noteQuery?: string
  sort?: Sort
  fallback?: boolean
  subjects?: string[]
  languageFilter?: string[]
  relevanceFilter?: string[]
  scrollId?: string
  draftStatus?: string[]
  users?: string[]
  grepCodes?: string[]
  aggregatePaths?: string[]
  embedResource?: string[]
  embedId?: string
  includeOtherStatuses?: boolean
  revisionDateFilterFrom?: string
  revisionDateFilterTo?: string
  excludeRevisionLog?: boolean
  responsibleIds?: string[]
  filterInactive?: boolean
  prioritized?: boolean
  priorityFilter?: string[]
  topics?: string[]
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
  learningResourceType: string
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

export interface IValidationError {
  code: string
  description: string
  messages: IValidationMessage[]
  occuredAt: string
}

export interface IValidationMessage {
  field: string
  message: string
}

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
}
