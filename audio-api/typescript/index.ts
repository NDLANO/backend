// DO NOT EDIT: generated file by scala-tsi

export interface IAudio {
  url: string
  mimeType: string
  fileSize: number
  language: string
}

export interface IAudioMetaInformation {
  id: number
  revision: number
  title: ITitle
  audioFile: IAudio
  copyright: ICopyright
  tags: ITag
  supportedLanguages: string[]
  audioType: string
  podcastMeta?: IPodcastMeta
  series?: ISeries
  manuscript?: IManuscript
  created: string
  updated: string
}

export interface IAudioSummary {
  id: number
  title: ITitle
  audioType: string
  url: string
  license: string
  supportedLanguages: string[]
  manuscript?: IManuscript
  podcastMeta?: IPodcastMeta
  series?: ISeriesSummary
  lastUpdated: string
}

export interface IAudioSummarySearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: IAudioSummary[]
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
  agreementId?: number
  validFrom?: string
  validTo?: string
}

export interface ICoverPhoto {
  id: string
  url: string
  altText: string
}

export interface IDescription {
  description: string
  language: string
}

export interface ILicense {
  license: string
  description?: string
  url?: string
}

export interface IManuscript {
  manuscript: string
  language: string
}

export interface INewAudioMetaInformation {
  title: string
  language: string
  copyright: ICopyright
  tags: string[]
  audioType?: string
  podcastMeta?: INewPodcastMeta
  seriesId?: number
  manuscript?: string
}

export interface INewPodcastMeta {
  introduction: string
  coverPhotoId: string
  coverPhotoAltText: string
}

export interface INewSeries {
  title: string
  description: string
  coverPhotoId: string
  coverPhotoAltText: string
  episodes: number[]
  language: string
  revision?: number
}

export interface IPodcastMeta {
  introduction: string
  coverPhoto: ICoverPhoto
  language: string
}

export interface ISearchParams {
  query?: string
  license?: string
  language?: string
  page?: number
  pageSize?: number
  sort?: string
  scrollId?: string
  audioType?: string
  filterBySeries?: boolean
}

export interface ISeries {
  id: number
  revision: number
  title: ITitle
  description: IDescription
  coverPhoto: ICoverPhoto
  episodes?: IAudioMetaInformation[]
  supportedLanguages: string[]
}

export interface ISeriesSearchParams {
  query?: string
  language?: string
  page?: number
  pageSize?: number
  sort?: string
  scrollId?: string
}

export interface ISeriesSummary {
  id: number
  title: ITitle
  description: IDescription
  supportedLanguages: string[]
  episodes?: IAudioSummary[]
  coverPhoto: ICoverPhoto
}

export interface ISeriesSummarySearchResult {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: ISeriesSummary[]
}

export interface ITag {
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

export interface ITitle {
  title: string
  language: string
}

export interface IUpdatedAudioMetaInformation {
  revision: number
  title: string
  language: string
  copyright: ICopyright
  tags: string[]
  audioType?: string
  podcastMeta?: INewPodcastMeta
  seriesId?: number
  manuscript?: string
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
