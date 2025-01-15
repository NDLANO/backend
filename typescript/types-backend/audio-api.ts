// DO NOT EDIT: generated file by scala-tsi

export enum AudioSortEnum {
  ByRelevanceDesc = "-relevance",
  ByRelevanceAsc = "relevance",
  ByTitleDesc = "-title",
  ByTitleAsc = "title",
  ByLastUpdatedDesc = "-lastUpdated",
  ByLastUpdatedAsc = "lastUpdated",
  ByIdDesc = "-id",
  ByIdAsc = "id",
}

export interface IAudioDTO {
  url: string
  mimeType: string
  fileSize: number
  language: string
}

export interface IAudioMetaInformationDTO {
  id: number
  revision: number
  title: ITitleDTO
  audioFile: IAudioDTO
  copyright: ICopyrightDTO
  tags: ITagDTO
  supportedLanguages: string[]
  audioType: string
  podcastMeta?: IPodcastMetaDTO
  series?: ISeriesDTO
  manuscript?: IManuscriptDTO
  created: string
  updated: string
}

export interface IAudioSummaryDTO {
  id: number
  title: ITitleDTO
  audioType: string
  url: string
  license: string
  supportedLanguages: string[]
  manuscript?: IManuscriptDTO
  podcastMeta?: IPodcastMetaDTO
  series?: ISeriesSummaryDTO
  lastUpdated: string
}

export interface IAudioSummarySearchResultDTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IAudioSummaryDTO[]
}

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

export interface ICoverPhotoDTO {
  id: string
  url: string
  altText: string
}

export interface IDescriptionDTO {
  description: string
  language: string
}

export interface ILicenseDTO {
  license: string
  description?: string
  url?: string
}

export interface IManuscriptDTO {
  manuscript: string
  language: string
}

export interface INewAudioMetaInformationDTO {
  title: string
  language: string
  copyright: ICopyrightDTO
  tags: string[]
  audioType?: string
  podcastMeta?: INewPodcastMetaDTO
  seriesId?: number
  manuscript?: string
}

export interface INewPodcastMetaDTO {
  introduction: string
  coverPhotoId: string
  coverPhotoAltText: string
}

export interface INewSeriesDTO {
  title: string
  description: string
  coverPhotoId: string
  coverPhotoAltText: string
  episodes: number[]
  language: string
  revision?: number
  hasRSS?: boolean
}

export interface IPodcastMetaDTO {
  introduction: string
  coverPhoto: ICoverPhotoDTO
  language: string
}

export interface ISearchParamsDTO {
  query?: string
  license?: string
  language?: string
  page?: number
  pageSize?: number
  sort?: Sort
  scrollId?: string
  audioType?: string
  filterBySeries?: boolean
  fallback?: boolean
}

export interface ISeriesDTO {
  id: number
  revision: number
  title: ITitleDTO
  description: IDescriptionDTO
  coverPhoto: ICoverPhotoDTO
  episodes?: IAudioMetaInformationDTO[]
  supportedLanguages: string[]
  hasRSS: boolean
}

export interface ISeriesSearchParamsDTO {
  query?: string
  language?: string
  page?: number
  pageSize?: number
  sort?: Sort
  scrollId?: string
  fallback?: boolean
}

export interface ISeriesSummaryDTO {
  id: number
  title: ITitleDTO
  description: IDescriptionDTO
  supportedLanguages: string[]
  episodes?: IAudioSummaryDTO[]
  coverPhoto: ICoverPhotoDTO
}

export interface ISeriesSummarySearchResultDTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: ISeriesSummaryDTO[]
}

export interface ITagDTO {
  tags: string[]
  language: string
}

export interface ITagsSearchResultDTO {
  totalCount: number
  page: number
  pageSize: number
  language: string
  results: string[]
}

export interface ITitleDTO {
  title: string
  language: string
}

export interface ITranscriptionResultDTO {
  status: string
  transcription?: string
}

export interface IUpdatedAudioMetaInformationDTO {
  revision: number
  title: string
  language: string
  copyright: ICopyrightDTO
  tags: string[]
  audioType?: string
  podcastMeta?: INewPodcastMetaDTO
  seriesId?: number
  manuscript?: string
}

export type Sort = AudioSortEnum
