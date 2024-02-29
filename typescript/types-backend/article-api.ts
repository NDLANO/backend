// DO NOT EDIT: generated file by scala-tsi

export type Availability = ("decoder" | "encoder" | "everyone" | "teacher")

export interface IArticleContentV2 {
  content: string
  language: string
}

export interface IArticleDump {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: IArticleV2[]
}

export interface IArticleIds {
  articleId: number
  externalIds: string[]
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

export interface IArticleSearchParams {
  query?: string
  language?: string
  license?: string
  page?: number
  pageSize?: number
  ids?: number[]
  articleTypes?: string[]
  sort?: string
  fallback?: boolean
  scrollId?: string
  grepCodes?: string[]
}

export interface IArticleSummaryV2 {
  id: number
  title: IArticleTitle
  visualElement?: IVisualElement
  introduction?: IArticleIntroduction
  metaDescription?: IArticleMetaDescription
  metaImage?: IArticleMetaImage
  url: string
  license: string
  articleType: string
  lastUpdated: string
  supportedLanguages: string[]
  grepCodes: string[]
  availability: string
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

export interface IArticleV2 {
  id: number
  oldNdlaUrl?: string
  revision: number
  title: IArticleTitle
  content: IArticleContentV2
  copyright: ICopyright
  tags: IArticleTag
  requiredLibraries: IRequiredLibrary[]
  visualElement?: IVisualElement
  metaImage?: IArticleMetaImage
  introduction?: IArticleIntroduction
  metaDescription: IArticleMetaDescription
  created: string
  updated: string
  updatedBy: string
  published: string
  articleType: string
  supportedLanguages: string[]
  grepCodes: string[]
  conceptIds: number[]
  availability: string
  relatedContent: (IRelatedContentLink | number)[]
  revisionDate?: string
  slug?: string
}

export interface IAuthor {
  type: string
  name: string
}

export interface ICopyright {
  license: ILicense
  origin?: string
  creators: IAuthor[]
  processors: IAuthor[]
  rightsholders: IAuthor[]
  validFrom?: string
  validTo?: string
  processed: boolean
}

export interface ILicense {
  license: string
  description?: string
  url?: string
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

export interface ISearchResultV2 {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IArticleSummaryV2[]
}

export interface ITagsSearchResult {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
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
