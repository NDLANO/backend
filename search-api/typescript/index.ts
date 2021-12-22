// DO NOT EDIT: generated file by scala-tsi

export interface IApiTaxonomyContext {
  id: string
  subject: string
  subjectId: string
  relevance: string
  path: string
  breadcrumbs: string[]
  filters: ITaxonomyContextFilter[]
  learningResourceType: string
  resourceTypes: ITaxonomyResourceType[]
  language: string
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
}

export interface IMultiSearchTermsAggregation {
  field: string
  sumOtherDocCount: number
  docCountErrorUpperBound: number
  values: ITermValue[]
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

export interface ITaxonomyContextFilter {
  id: string
  name: string
  relevance: string
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
