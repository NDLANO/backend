// DO NOT EDIT: generated file by scala-tsi

export enum ConceptSortEnum {
  ByRelevanceDesc = "-relevance",
  ByRelevanceAsc = "relevance",
  ByTitleDesc = "-title",
  ByTitleAsc = "title",
  ByLastUpdatedDesc = "-lastUpdated",
  ByLastUpdatedAsc = "lastUpdated",
  ByIdDesc = "-id",
  ByIdAsc = "id",
  ByResponsibleLastUpdatedDesc = "-responsibleLastUpdated",
  ByResponsibleLastUpdatedAsc = "responsibleLastUpdated",
  ByStatusAsc = "status",
  ByStatusDesc = "-status",
  BySubjectAsc = "subject",
  BySubjectDesc = "-subject",
  ByConceptTypeAsc = "conceptType",
  ByConceptTypeDesc = "-conceptType",
}

export interface IAuthorDTO {
  type: string
  name: string
}

export interface IConceptContent {
  content: string
  htmlContent: string
  language: string
}

export interface IConceptDTO {
  id: number
  revision: number
  title: IConceptTitleDTO
  content?: IConceptContent
  copyright?: IDraftCopyrightDTO
  source?: string
  metaImage?: IConceptMetaImageDTO
  tags?: IConceptTagsDTO
  created: string
  updated: string
  updatedBy?: string[]
  supportedLanguages: string[]
  status: IStatusDTO
  visualElement?: IVisualElementDTO
  responsible?: IConceptResponsibleDTO
  conceptType: string
  glossData?: IGlossDataDTO
  editorNotes?: IEditorNoteDTO[]
}

export interface IConceptMetaImageDTO {
  url: string
  alt: string
  language: string
}

export interface IConceptResponsibleDTO {
  responsibleId: string
  lastUpdated: string
}

export interface IConceptSearchParamsDTO {
  query?: string
  language?: string
  page?: number
  pageSize?: number
  ids?: number[]
  sort?: Sort
  fallback?: boolean
  scrollId?: string
  tags?: string[]
  exactMatch?: boolean
  embedResource?: string[]
  embedId?: string
  conceptType?: string
  aggregatePaths?: string[]
}

export interface IConceptSearchResultDTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IConceptSummaryDTO[]
  aggregations: IMultiSearchTermsAggregationDTO[]
}

export interface IConceptSummaryDTO {
  id: number
  title: IConceptTitleDTO
  content: IConceptContent
  metaImage: IConceptMetaImageDTO
  tags?: IConceptTagsDTO
  supportedLanguages: string[]
  lastUpdated: string
  created: string
  status: IStatusDTO
  updatedBy: string[]
  license?: string
  copyright?: IDraftCopyrightDTO
  visualElement?: IVisualElementDTO
  source?: string
  responsible?: IConceptResponsibleDTO
  conceptType: string
  glossData?: IGlossDataDTO
  conceptTypeName: string
}

export interface IConceptTagsDTO {
  tags: string[]
  language: string
}

export interface IConceptTitleDTO {
  title: string
  language: string
}

export interface IDraftConceptSearchParamsDTO {
  query?: string
  language?: string
  page?: number
  pageSize?: number
  ids?: number[]
  sort?: Sort
  fallback?: boolean
  scrollId?: string
  tags?: string[]
  status?: string[]
  users?: string[]
  embedResource?: string[]
  embedId?: string
  responsibleIds?: string[]
  conceptType?: string
  aggregatePaths?: string[]
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

export interface IEditorNoteDTO {
  note: string
  updatedBy: string
  status: IStatusDTO
  timestamp: string
}

export interface IGlossDataDTO {
  gloss: string
  wordClass: string
  originalLanguage: string
  transcriptions: { [ key: string ]: string }
  examples: IGlossExampleDTO[][]
}

export interface IGlossExampleDTO {
  example: string
  language: string
  transcriptions: { [ key: string ]: string }
}

export interface ILicenseDTO {
  license: string
  description?: string
  url?: string
}

export interface IMultiSearchTermsAggregationDTO {
  field: string
  sumOtherDocCount: number
  docCountErrorUpperBound: number
  values: ITermValueDTO[]
}

export interface INewConceptDTO {
  language: string
  title: string
  content?: string
  copyright?: IDraftCopyrightDTO
  metaImage?: INewConceptMetaImageDTO
  tags?: string[]
  visualElement?: string
  responsibleId?: string
  conceptType: string
  glossData?: IGlossDataDTO
}

export interface INewConceptMetaImageDTO {
  id: string
  alt: string
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

export interface ITermValueDTO {
  value: string
  count: number
}

export interface IUpdatedConceptDTO {
  language: string
  title?: string
  content?: string
  metaImage: UpdateOrDeleteNewConceptMetaImageDTO
  copyright?: IDraftCopyrightDTO
  tags?: string[]
  status?: string
  visualElement?: string
  responsibleId: UpdateOrDeleteString
  conceptType?: string
  glossData?: IGlossDataDTO
}

export interface IVisualElementDTO {
  visualElement: string
  language: string
}

export type Sort = ConceptSortEnum

export type UpdateOrDeleteNewConceptMetaImageDTO = (null | undefined | INewConceptMetaImageDTO)

export type UpdateOrDeleteString = (null | undefined | string)
