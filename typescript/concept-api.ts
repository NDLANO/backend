// DO NOT EDIT: generated file by scala-tsi

export interface IAuthor {
  type: string
  name: string
}

export interface IConcept {
  id: number
  revision: number
  title: IConceptTitle
  content?: IConceptContent
  copyright?: IDraftCopyright
  source?: string
  metaImage?: IConceptMetaImage
  tags?: IConceptTags
  subjectIds?: string[]
  created: string
  updated: string
  updatedBy?: string[]
  supportedLanguages: string[]
  articleIds: number[]
  status: IStatus
  visualElement?: IVisualElement
  responsible?: IConceptResponsible
  conceptType: string
  glossData?: IGlossData
  editorNotes?: IEditorNote[]
}

export interface IConceptContent {
  content: string
  htmlContent: string
  language: string
}

export interface IConceptMetaImage {
  url: string
  alt: string
  language: string
}

export interface IConceptResponsible {
  responsibleId: string
  lastUpdated: string
}

export interface IConceptSearchParams {
  query?: string
  language?: string
  page?: number
  pageSize?: number
  idList: number[]
  sort?: string
  fallback?: boolean
  scrollId?: string
  subjects: string[]
  tags: string[]
  exactTitleMatch?: boolean
  embedResource?: string
  embedId?: string
  conceptType?: string
  aggregatePaths: string[]
}

export interface IConceptSearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IConceptSummary[]
  aggregations: IMultiSearchTermsAggregation[]
}

export interface IConceptSummary {
  id: number
  title: IConceptTitle
  content: IConceptContent
  metaImage: IConceptMetaImage
  tags?: IConceptTags
  subjectIds?: string[]
  supportedLanguages: string[]
  lastUpdated: string
  created: string
  status: IStatus
  updatedBy: string[]
  license?: string
  copyright?: IDraftCopyright
  visualElement?: IVisualElement
  articleIds: number[]
  source?: string
  responsible?: IConceptResponsible
  conceptType: string
  glossData?: IGlossData
  subjectName?: string
  conceptTypeName: string
}

export interface IConceptTags {
  tags: string[]
  language: string
}

export interface IConceptTitle {
  title: string
  language: string
}

export interface IDraftConceptSearchParams {
  query?: string
  language?: string
  page?: number
  pageSize?: number
  idList: number[]
  sort?: string
  fallback?: boolean
  scrollId?: string
  subjects: string[]
  tags: string[]
  status: string[]
  users: string[]
  embedResource?: string
  embedId?: string
  responsibleIds: string[]
  conceptType?: string
  aggregatePaths: string[]
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

export interface IEditorNote {
  note: string
  updatedBy: string
  status: IStatus
  timestamp: string
}

export interface IGlossData {
  gloss: string
  wordClass: string
  originalLanguage: string
  transcriptions: { [ key: string ]: string }
  examples: IGlossExample[][]
}

export interface IGlossExample {
  example: string
  language: string
  transcriptions: { [ key: string ]: string }
}

export interface ILicense {
  license: string
  description?: string
  url?: string
}

export interface IMultiSearchTermsAggregation {
  field: string
  sumOtherDocCount: number
  docCountErrorUpperBound: number
  values: ITermValue[]
}

export interface INewConcept {
  language: string
  title: string
  content?: string
  copyright?: IDraftCopyright
  metaImage?: INewConceptMetaImage
  tags?: string[]
  subjectIds?: string[]
  articleIds?: number[]
  visualElement?: string
  responsibleId?: string
  conceptType: string
  glossData?: IGlossData
}

export interface INewConceptMetaImage {
  id: string
  alt: string
}

export interface IStatus {
  current: string
  other: string[]
}

export interface ISubjectTags {
  subjectId: string
  tags: string[]
  language: string
}

export interface ITagsSearchResult {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface ITermValue {
  value: string
  count: number
}

export interface IUpdatedConcept {
  language: string
  title?: string
  content?: string
  metaImage?: (null | INewConceptMetaImage)
  copyright?: IDraftCopyright
  tags?: string[]
  subjectIds?: string[]
  articleIds?: number[]
  status?: string
  visualElement?: string
  responsibleId?: (null | string)
  conceptType?: string
  glossData?: IGlossData
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

export interface IVisualElement {
  visualElement: string
  language: string
}
