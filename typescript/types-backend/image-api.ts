// DO NOT EDIT: generated file by scala-tsi

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

export interface IEditorNote {
  timestamp: string
  updatedBy: string
  note: string
}

export interface IImage {
  url: string
  size: number
  contentType: string
}

export interface IImageAltText {
  alttext: string
  language: string
}

export interface IImageCaption {
  caption: string
  language: string
}

export interface IImageDimensions {
  width: number
  height: number
}

export interface IImageFile {
  fileName: string
  size: number
  contentType: string
  imageUrl: string
  dimensions?: IImageDimensions
  language: string
}

export interface IImageMetaInformationV2 {
  id: string
  metaUrl: string
  title: IImageTitle
  alttext: IImageAltText
  imageUrl: string
  size: number
  contentType: string
  copyright: ICopyright
  tags: IImageTag
  caption: IImageCaption
  supportedLanguages: string[]
  created: string
  createdBy: string
  modelRelease: string
  editorNotes?: IEditorNote[]
  imageDimensions?: IImageDimensions
}

export interface IImageMetaInformationV3 {
  id: string
  metaUrl: string
  title: IImageTitle
  alttext: IImageAltText
  copyright: ICopyright
  tags: IImageTag
  caption: IImageCaption
  supportedLanguages: string[]
  created: string
  createdBy: string
  modelRelease: string
  editorNotes?: IEditorNote[]
  image: IImageFile
}

export interface IImageMetaSummary {
  id: string
  title: IImageTitle
  contributors: string[]
  altText: IImageAltText
  caption: IImageCaption
  previewUrl: string
  metaUrl: string
  license: string
  supportedLanguages: string[]
  modelRelease?: string
  editorNotes?: string[]
  lastUpdated: string
  fileSize: number
  contentType: string
  imageDimensions?: IImageDimensions
}

export interface IImageTag {
  tags: string[]
  language: string
}

export interface IImageTitle {
  title: string
  language: string
}

export interface ILicense {
  license: string
  description?: string
  url?: string
}

export interface INewImageMetaInformationV2 {
  title: string
  alttext?: string
  copyright: ICopyright
  tags: string[]
  caption: string
  language: string
  modelReleased?: string
}

export interface ISearchParams {
  query?: string
  license?: string
  language?: string
  fallback?: boolean
  minimumSize?: number
  includeCopyrighted?: boolean
  sort?: string
  page?: number
  pageSize?: number
  podcastFriendly?: boolean
  scrollId?: string
  modelReleased?: string[]
}

export interface ISearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IImageMetaSummary[]
}

export interface ISearchResultV3 {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IImageMetaInformationV3[]
}

export interface ITagsSearchResult {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface IUpdateImageMetaInformation {
  language: string
  title?: string
  alttext: UpdateOrDeleteString
  copyright?: ICopyright
  tags?: string[]
  caption?: string
  modelReleased?: string
}

export interface IValidationError {
  code: string
  description: string
  messages: IValidationMessage[]
  occurredAt: string
}

export interface IValidationMessage {
  field: string
  message: string
}

export type UpdateOrDeleteString = (null | undefined | string)
