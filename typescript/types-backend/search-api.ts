// DO NOT EDIT: generated file by scala-tsi

export type GrepResultDTO = (IGrepKjerneelementDTO | IGrepKompetansemaalDTO | IGrepKompetansemaalSettDTO | IGrepLaererplanDTO | IGrepTverrfagligTemaDTO)

export type GrepSort = ("-relevance" | "relevance" | "-title" | "title" | "-code" | "code")

export interface IApiTaxonomyContextDTO {
  publicId: string
  root: string
  rootId: string
  relevance: string
  relevanceId: string
  path: string
  breadcrumbs: string[]
  contextId: string
  contextType: string
  resourceTypes: ITaxonomyResourceTypeDTO[]
  language: string
  isPrimary: boolean
  isActive: boolean
  url: string
}

export interface IArticleIntroductionDTO {
  introduction: string
  htmlIntroduction: string
  language: string
}

export interface IArticleResultDTO {
  id: number
  title: ITitleWithHtmlDTO
  introduction?: IArticleIntroductionDTO
  articleType: string
  supportedLanguages: string[]
}

export interface IArticleResultsDTO {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: IArticleResultDTO[]
}

export interface IAudioResultDTO {
  id: number
  title: ITitleDTO
  url: string
  supportedLanguages: string[]
}

export interface IAudioResultsDTO {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: IAudioResultDTO[]
}

export interface ICommentDTO {
  id: string
  content: string
  created: string
  updated: string
  isOpen: boolean
  solved: boolean
}

export interface IDescriptionDTO {
  description: string
  language: string
}

export interface IDraftResponsibleDTO {
  responsibleId: string
  lastUpdated: string
}

export interface IDraftSearchParamsDTO {
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
  traits?: SearchTrait[]
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

export interface IGrepKjerneelementDTO {
  code: string
  title: ITitleDTO
  description: IDescriptionDTO
  laereplan: IGrepReferencedLaereplanDTO
  typename: "GrepKjerneelementDTO"
}

export interface IGrepKompetansemaalDTO {
  code: string
  title: ITitleDTO
  laereplan: IGrepReferencedLaereplanDTO
  kompetansemaalSett: IGrepReferencedKompetansemaalSettDTO
  tverrfagligeTemaer: IGrepTverrfagligTemaDTO[]
  kjerneelementer: IGrepReferencedKjerneelementDTO[]
  reuseOf?: IGrepReferencedKompetansemaalDTO
  typename: "GrepKompetansemaalDTO"
}

export interface IGrepKompetansemaalSettDTO {
  code: string
  title: ITitleDTO
  kompetansemaal: IGrepReferencedKompetansemaalDTO[]
  typename: "GrepKompetansemaalSettDTO"
}

export interface IGrepLaererplanDTO {
  code: string
  title: ITitleDTO
  replacedBy: IGrepReferencedLaereplanDTO[]
  typename: "GrepLaererplanDTO"
}

export interface IGrepReferencedKjerneelementDTO {
  code: string
  title: string
}

export interface IGrepReferencedKompetansemaalDTO {
  code: string
  title: string
}

export interface IGrepReferencedKompetansemaalSettDTO {
  code: string
  title: string
}

export interface IGrepReferencedLaereplanDTO {
  code: string
  title: string
}

export interface IGrepSearchInputDTO {
  prefixFilter?: string[]
  codes?: string[]
  query?: string
  page?: number
  pageSize?: number
  sort?: GrepSort
  language?: string
}

export interface IGrepSearchResultsDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: GrepResultDTO[]
}

export interface IGrepTverrfagligTemaDTO {
  code: string
  title: ITitleDTO
  typename: "GrepTverrfagligTemaDTO"
}

export interface IGroupSearchResultDTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IMultiSearchSummaryDTO[]
  suggestions: IMultiSearchSuggestionDTO[]
  aggregations: IMultiSearchTermsAggregationDTO[]
  resourceType: string
}

export interface IHighlightedFieldDTO {
  field: string
  matches: string[]
}

export interface IImageAltTextDTO {
  altText: string
  language: string
}

export interface IImageResultDTO {
  id: number
  title: ITitleDTO
  altText: IImageAltTextDTO
  previewUrl: string
  metaUrl: string
  supportedLanguages: string[]
}

export interface IImageResultsDTO {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: IImageResultDTO[]
}

export interface ILearningPathIntroductionDTO {
  introduction: string
  language: string
}

export interface ILearningpathResultDTO {
  id: number
  title: ITitleDTO
  introduction: ILearningPathIntroductionDTO
  supportedLanguages: string[]
}

export interface ILearningpathResultsDTO {
  type: string
  language: string
  totalCount: number
  page: number
  pageSize: number
  results: ILearningpathResultDTO[]
}

export interface IMetaDescriptionDTO {
  metaDescription: string
  language: string
}

export interface IMetaImageDTO {
  url: string
  alt: string
  language: string
}

export interface IMultiSearchResultDTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IMultiSearchSummaryDTO[]
  suggestions: IMultiSearchSuggestionDTO[]
  aggregations: IMultiSearchTermsAggregationDTO[]
}

export interface IMultiSearchSuggestionDTO {
  name: string
  suggestions: ISearchSuggestionDTO[]
}

export interface IMultiSearchSummaryDTO {
  id: number
  title: ITitleWithHtmlDTO
  metaDescription: IMetaDescriptionDTO
  metaImage?: IMetaImageDTO
  url: string
  contexts: IApiTaxonomyContextDTO[]
  supportedLanguages: string[]
  learningResourceType: LearningResourceType
  status?: IStatusDTO
  traits: SearchTrait[]
  score: number
  highlights: IHighlightedFieldDTO[]
  paths: string[]
  lastUpdated: string
  license?: string
  revisions: IRevisionMetaDTO[]
  responsible?: IDraftResponsibleDTO
  comments?: ICommentDTO[]
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

export interface IMultiSearchTermsAggregationDTO {
  field: string
  sumOtherDocCount: number
  docCountErrorUpperBound: number
  values: ITermValueDTO[]
}

export interface IRevisionMetaDTO {
  revisionDate: string
  note: string
  status: string
}

export interface ISearchParamsDTO {
  page?: number
  pageSize?: number
  articleTypes?: string[]
  scrollId?: string
  query?: string
  fallback?: boolean
  language?: string
  license?: string
  ids?: number[]
  subjects?: string[]
  resourceTypes?: string[]
  contextTypes?: string[]
  relevance?: string[]
  languageFilter?: string[]
  grepCodes?: string[]
  traits?: SearchTrait[]
  aggregatePaths?: string[]
  embedResource?: string[]
  embedId?: string
  filterInactive?: boolean
  sort?: string
}

export interface ISearchSuggestionDTO {
  text: string
  offset: number
  length: number
  options: ISuggestOptionDTO[]
}

export interface IStatusDTO {
  current: string
  other: string[]
}

export interface ISubjectAggregationDTO {
  subjectId: string
  publishedArticleCount: number
  oldArticleCount: number
  revisionCount: number
  flowCount: number
  favoritedCount: number
}

export interface ISubjectAggregationsDTO {
  subjects: ISubjectAggregationDTO[]
}

export interface ISubjectAggsInputDTO {
  subjects?: string[]
}

export interface ISuggestOptionDTO {
  text: string
  score: number
}

export interface ITaxonomyResourceTypeDTO {
  id: string
  name: string
  language: string
}

export interface ITermValueDTO {
  value: string
  count: number
}

export interface ITitleDTO {
  title: string
  language: string
}

export interface ITitleWithHtmlDTO {
  title: string
  htmlTitle: string
  language: string
}

export type LearningResourceType = ("standard" | "topic-article" | "frontpage-article" | "learningpath" | "concept" | "gloss")

export type SearchTrait = ("VIDEO" | "H5P" | "AUDIO" | "PODCAST")

export type SearchType = ("article" | "draft" | "learningpath" | "concept" | "grep")

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
