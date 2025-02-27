// DO NOT EDIT: generated file by scala-tsi

export enum ArticleSortEnum {
  ByRelevanceDesc = "-relevance",
  ByRelevanceAsc = "relevance",
  ByTitleDesc = "-title",
  ByTitleAsc = "title",
  ByLastUpdatedDesc = "-lastUpdated",
  ByLastUpdatedAsc = "lastUpdated",
  ByIdDesc = "-id",
  ByIdAsc = "id",
}

export type Availability = ("everyone" | "teacher")

export type ContributorType = ("artist" | "cowriter" | "compiler" | "composer" | "correction" | "director" | "distributor" | "editorial" | "facilitator" | "idea" | "illustrator" | "linguistic" | "originator" | "photographer" | "processor" | "publisher" | "reader" | "rightsholder" | "scriptwriter" | "supplier" | "translator" | "writer")

export interface IArticleContentV2DTO {
  content: string
  language: string
}

export interface IArticleDumpDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: IArticleV2DTO[]
}

export interface IArticleIdsDTO {
  articleId: number
  externalIds: string[]
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
  fallback?: boolean
  scrollId?: string
  grepCodes?: string[]
}

export interface IArticleSummaryV2DTO {
  id: number
  title: IArticleTitleDTO
  visualElement?: IVisualElementDTO
  introduction?: IArticleIntroductionDTO
  metaDescription?: IArticleMetaDescriptionDTO
  metaImage?: IArticleMetaImageDTO
  url: string
  license: string
  articleType: string
  lastUpdated: string
  supportedLanguages: string[]
  grepCodes: string[]
  availability: string
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

export interface IArticleV2DTO {
  id: number
  oldNdlaUrl?: string
  revision: number
  title: IArticleTitleDTO
  content: IArticleContentV2DTO
  copyright: ICopyrightDTO
  tags: IArticleTagDTO
  requiredLibraries: IRequiredLibraryDTO[]
  visualElement?: IVisualElementDTO
  metaImage?: IArticleMetaImageDTO
  introduction?: IArticleIntroductionDTO
  metaDescription: IArticleMetaDescriptionDTO
  created: string
  updated: string
  updatedBy: string
  published: string
  articleType: string
  supportedLanguages: string[]
  grepCodes: string[]
  conceptIds: number[]
  availability: string
  relatedContent: (IRelatedContentLinkDTO | number)[]
  revisionDate?: string
  slug?: string
  disclaimer?: IDisclaimerDTO
}

export interface IAuthorDTO {
  type: ContributorType
  name: string
}

export interface ICopyrightDTO {
  license: ILicenseDTO
  origin?: string
  creators: IAuthorDTO[]
  processors: IAuthorDTO[]
  rightsholders: IAuthorDTO[]
  validFrom?: string
  validTo?: string
  processed: boolean
}

export interface IDisclaimerDTO {
  disclaimer: string
  language: string
}

export interface ILicenseDTO {
  license: string
  description?: string
  url?: string
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

export interface ISearchResultV2DTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IArticleSummaryV2DTO[]
}

export interface ITagsSearchResultDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface IVisualElementDTO {
  visualElement: string
  language: string
}

export type Sort = ArticleSortEnum
