// DO NOT EDIT: generated file by scala-tsi

export interface IAboutFilmSubjectDTO {
  title: string
  description: string
  visualElement: IVisualElementDTO
  language: string
}

export interface IAboutSubjectDTO {
  title: string
  description: string
  visualElement: IVisualElementDTO
}

export interface IBannerImageDTO {
  mobileUrl?: string
  mobileId?: number
  desktopUrl: string
  desktopId: number
}

export interface IErrorBody {
  code: string
  description: string
  occurredAt: string
  statusCode: number
}

export interface IFilmFrontPageDataDTO {
  name: string
  about: IAboutFilmSubjectDTO[]
  movieThemes: IMovieThemeDTO[]
  slideShow: string[]
  article?: string
}

export interface IFrontPageDTO {
  articleId: number
  menu: IMenuDTO[]
}

export interface IMenuDTO {
  articleId: number
  menu: IMenuDataDTO[]
  hideLevel?: boolean
}

export type IMenuDataDTO = IMenuDTO

export interface IMovieThemeDTO {
  name: IMovieThemeNameDTO[]
  movies: string[]
}

export interface IMovieThemeNameDTO {
  name: string
  language: string
}

export interface INewOrUpdateBannerImageDTO {
  mobileImageId?: number
  desktopImageId: number
}

export interface INewOrUpdatedAboutSubjectDTO {
  title: string
  description: string
  language: string
  visualElement: INewOrUpdatedVisualElementDTO
}

export interface INewOrUpdatedFilmFrontPageDataDTO {
  name: string
  about: INewOrUpdatedAboutSubjectDTO[]
  movieThemes: INewOrUpdatedMovieThemeDTO[]
  slideShow: string[]
  article?: string
}

export interface INewOrUpdatedMetaDescriptionDTO {
  metaDescription: string
  language: string
}

export interface INewOrUpdatedMovieNameDTO {
  name: string
  language: string
}

export interface INewOrUpdatedMovieThemeDTO {
  name: INewOrUpdatedMovieNameDTO[]
  movies: string[]
}

export interface INewOrUpdatedVisualElementDTO {
  type: string
  id: string
  alt?: string
}

export interface INewSubjectFrontPageDataDTO {
  name: string
  externalId?: string
  banner: INewOrUpdateBannerImageDTO
  about: INewOrUpdatedAboutSubjectDTO[]
  metaDescription: INewOrUpdatedMetaDescriptionDTO[]
  editorsChoices?: string[]
  connectedTo?: string[]
  buildsOn?: string[]
  leadsTo?: string[]
}

export interface ISubjectPageDataDTO {
  id: number
  name: string
  banner: IBannerImageDTO
  about?: IAboutSubjectDTO
  metaDescription?: string
  editorsChoices: string[]
  supportedLanguages: string[]
  connectedTo: string[]
  buildsOn: string[]
  leadsTo: string[]
}

export interface IUpdatedSubjectFrontPageDataDTO {
  name?: string
  externalId?: string
  banner?: INewOrUpdateBannerImageDTO
  about?: INewOrUpdatedAboutSubjectDTO[]
  metaDescription?: INewOrUpdatedMetaDescriptionDTO[]
  editorsChoices?: string[]
  connectedTo?: string[]
  buildsOn?: string[]
  leadsTo?: string[]
}

export interface IVisualElementDTO {
  type: string
  url: string
  alt?: string
}
