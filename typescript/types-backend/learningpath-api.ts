// DO NOT EDIT: generated file by scala-tsi

export interface IAuthorDTO {
  type: string
  name: string
}

export interface IConfigMetaDTO {
  key: string
  value: (boolean | string[])
  updatedAt: string
  updatedBy: string
}

export interface IConfigMetaRestrictedDTO {
  key: string
  value: (boolean | string[])
}

export interface ICopyrightDTO {
  license: ILicenseDTO
  contributors: IAuthorDTO[]
}

export interface ICoverPhotoDTO {
  url: string
  metaUrl: string
}

export interface IDescriptionDTO {
  description: string
  language: string
}

export interface IEmbedUrlV2DTO {
  url: string
  embedType: string
}

export interface IIntroductionDTO {
  introduction: string
  language: string
}

export interface ILearningPathStatusDTO {
  status: string
}

export interface ILearningPathSummaryV2DTO {
  id: number
  revision?: number
  title: ITitleDTO
  description: IDescriptionDTO
  introduction: IIntroductionDTO
  metaUrl: string
  coverPhotoUrl?: string
  duration?: number
  status: string
  created: string
  lastUpdated: string
  tags: ILearningPathTagsDTO
  copyright: ICopyrightDTO
  supportedLanguages: string[]
  isBasedOn?: number
  message?: string
}

export interface ILearningPathTagsDTO {
  tags: string[]
  language: string
}

export interface ILearningPathTagsSummaryDTO {
  language: string
  supportedLanguages: string[]
  tags: string[]
}

export interface ILearningPathV2DTO {
  id: number
  revision: number
  isBasedOn?: number
  title: ITitleDTO
  description: IDescriptionDTO
  metaUrl: string
  learningsteps: ILearningStepV2DTO[]
  learningstepUrl: string
  coverPhoto?: ICoverPhotoDTO
  duration?: number
  status: string
  verificationStatus: string
  created: string
  lastUpdated: string
  tags: ILearningPathTagsDTO
  copyright: ICopyrightDTO
  canEdit: boolean
  supportedLanguages: string[]
  ownerId?: string
  message?: IMessageDTO
  madeAvailable?: string
}

export interface ILearningStepContainerSummaryDTO {
  language: string
  learningsteps: ILearningStepSummaryV2DTO[]
  supportedLanguages: string[]
}

export interface ILearningStepSeqNoDTO {
  seqNo: number
}

export interface ILearningStepStatusDTO {
  status: string
}

export interface ILearningStepSummaryV2DTO {
  id: number
  seqNo: number
  title: ITitleDTO
  type: string
  metaUrl: string
}

export interface ILearningStepV2DTO {
  id: number
  revision: number
  seqNo: number
  title: ITitleDTO
  introduction?: IIntroductionDTO
  description?: IDescriptionDTO
  embedUrl?: IEmbedUrlV2DTO
  showTitle: boolean
  type: string
  license?: ILicenseDTO
  metaUrl: string
  canEdit: boolean
  status: string
  supportedLanguages: string[]
}

export interface ILicenseDTO {
  license: string
  description?: string
  url?: string
}

export interface IMessageDTO {
  message: string
  date: string
}

export interface ISearchResultV2DTO {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: ILearningPathSummaryV2DTO[]
}

export interface ITitleDTO {
  title: string
  language: string
}
