// DO NOT EDIT: generated file by scala-tsi

export interface IAudio {
  url: string
  mimeType: string
  fileSize: number
  language: string
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

export interface ILicense {
  license: string
  description?: string
  url?: string
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
