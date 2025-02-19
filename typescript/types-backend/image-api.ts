// DO NOT EDIT: generated file by scala-tsi

export interface IAuthorDTO {
  type: string
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

export interface IEditorNoteDTO {
  timestamp: string
  updatedBy: string
  note: string
}

export interface IImageAltTextDTO {
  alttext: string
  language: string
}

export interface IImageCaptionDTO {
  caption: string
  language: string
}

export interface IImageDTO {
  url: string
  size: number
  contentType: string
}

export interface IImageDimensionsDTO {
  width: number
  height: number
}

export interface IImageFileDTO {
  fileName: string
  size: number
  contentType: string
  imageUrl: string
  dimensions?: IImageDimensionsDTO
  language: string
}

export interface IImageMetaInformationV2DTO {
  id: string
  metaUrl: string
  title: IImageTitleDTO
  alttext: IImageAltTextDTO
  imageUrl: string
  size: number
  contentType: string
  copyright: ICopyrightDTO
  tags: IImageTagDTO
  caption: IImageCaptionDTO
  supportedLanguages: string[]
  created: string
  createdBy: string
  modelRelease: string
  editorNotes?: IEditorNoteDTO[]
  imageDimensions?: IImageDimensionsDTO
}

export interface IImageMetaInformationV3DTO {
  id: string
  metaUrl: string
  title: IImageTitleDTO
  alttext: IImageAltTextDTO
  copyright: ICopyrightDTO
  tags: IImageTagDTO
  caption: IImageCaptionDTO
  supportedLanguages: string[]
  created: string
  createdBy: string
  modelRelease: string
  editorNotes?: IEditorNoteDTO[]
  image: IImageFileDTO
}

export interface IImageMetaSummaryDTO {
  id: string
  title: IImageTitleDTO
  contributors: string[]
  altText: IImageAltTextDTO
  caption: IImageCaptionDTO
  previewUrl: string
  metaUrl: string
  license: string
  supportedLanguages: string[]
  modelRelease?: string
  editorNotes?: string[]
  lastUpdated: string
  fileSize: number
  contentType: string
  imageDimensions?: IImageDimensionsDTO
}

export interface IImageTagDTO {
  tags: string[]
  language: string
}

export interface IImageTitleDTO {
  title: string
  language: string
}

export interface ILicenseDTO {
  license: string
  description?: string
  url?: string
}

export interface INewImageMetaInformationV2DTO {
  title: string
  alttext?: string
  copyright: ICopyrightDTO
  tags: string[]
  caption: string
  language: string
  modelReleased?: string
}

export interface ISearchParamsDTO {
  query?: string
  license?: string
  language?: string
  fallback?: boolean
  minimumSize?: number
  includeCopyrighted?: boolean
  sort?: Sort
  page?: number
  pageSize?: number
  podcastFriendly?: boolean
  scrollId?: string
  modelReleased?: string[]
  users?: string[]
}

export interface ISearchResultDTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IImageMetaSummaryDTO[]
}

export interface ISearchResultV3DTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IImageMetaInformationV3DTO[]
}

export interface ITagsSearchResultDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface IUpdateImageMetaInformationDTO {
  language: string
  title?: string
  alttext: UpdateOrDeleteString
  copyright?: ICopyrightDTO
  tags?: string[]
  caption?: string
  modelReleased?: string
}

export enum ImageSortEnum {
  ByRelevanceDesc = "-relevance",
  ByRelevanceAsc = "relevance",
  ByTitleDesc = "-title",
  ByTitleAsc = "title",
  ByLastUpdatedDesc = "-lastUpdated",
  ByLastUpdatedAsc = "lastUpdated",
  ByIdDesc = "-id",
  ByIdAsc = "id",
}

export type Sort = ImageSortEnum

export type UpdateOrDeleteString = (null | undefined | string)
