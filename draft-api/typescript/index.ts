// DO NOT EDIT: generated file by scala-tsi

export type Availability = ("everyone" | "teacher")

export interface IAgreement {
  id: number
  title: string
  content: string
  copyright: ICopyright
  created: string
  updated: string
  updatedBy: string
}

export interface IAgreementSearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IAgreementSummary[]
}

export interface IAgreementSummary {
  id: number
  title: string
  license: string
}

export interface IArticle {
  id: number
  oldNdlaUrl?: string
  revision: number
  status: IStatus
  title?: IArticleTitle
  content?: IArticleContent
  copyright?: ICopyright
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
}

export interface IArticleContent {
  content: string
  language: string
}

export interface IArticleIntroduction {
  introduction: string
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
}

export interface IArticleTag {
  tags: string[]
  language: string
}

export interface IArticleTitle {
  title: string
  language: string
}

export interface IAuthor {
  type: string
  name: string
}

export interface ICopyright {
  license?: ILicense
  origin?: string
  creators: IAuthor[]
  processors: IAuthor[]
  rightsholders: IAuthor[]
  agreementId?: number
  validFrom?: string
  validTo?: string
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

export interface INewAgreement {
  title: string
  content: string
  copyright: INewAgreementCopyright
}

export interface INewAgreementCopyright {
  license?: ILicense
  origin?: string
  creators: IAuthor[]
  processors: IAuthor[]
  rightsholders: IAuthor[]
  agreementId?: number
  validFrom?: string
  validTo?: string
}

export interface INewArticle {
  language: string
  title: string
  published?: string
  content?: string
  tags: string[]
  introduction?: string
  metaDescription?: string
  metaImage?: INewArticleMetaImage
  visualElement?: string
  copyright?: ICopyright
  requiredLibraries: IRequiredLibrary[]
  articleType: string
  notes: string[]
  editorLabels: string[]
  grepCodes: string[]
  conceptIds: number[]
  availability?: string
  relatedContent: (IRelatedContentLink | number)[]
  revisionMeta?: IRevisionMeta[]
}

export interface INewArticleMetaImage {
  id: string
  alt: string
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
  revisionDate: string
  note: string
  status: string
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

export interface IUpdatedAgreement {
  title?: string
  content?: string
  copyright?: INewAgreementCopyright
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
  metaImage?: (null | INewArticleMetaImage)
  visualElement?: string
  copyright?: ICopyright
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
}

export interface IUpdatedUserData {
  savedSearches?: string[]
  latestEditedArticles?: string[]
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
  savedSearches?: string[]
  latestEditedArticles?: string[]
  favoriteSubjects?: string[]
}

export interface IVisualElement {
  visualElement: string
  language: string
}
