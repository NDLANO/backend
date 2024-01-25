// DO NOT EDIT: generated file by scala-tsi

export interface IAboutFilmSubject {
  title: string
  description: string
  visualElement: IVisualElement
  language: string
}

export interface IAboutSubject {
  title: string
  description: string
  visualElement: IVisualElement
}

export interface IBannerImage {
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

export interface IFilmFrontPageData {
  name: string
  about: IAboutFilmSubject[]
  movieThemes: IMovieTheme[]
  slideShow: string[]
  article?: string
}

export interface IFrontPage {
  articleId: number
  menu: IMenu[]
}

export interface IMenu {
  articleId: number
  menu: IMenuData[]
  hideLevel: boolean
}

export type IMenuData = IMenu

export interface IMovieTheme {
  name: IMovieThemeName[]
  movies: string[]
}

export interface IMovieThemeName {
  name: string
  language: string
}

export interface INewOrUpdateBannerImage {
  mobileImageId?: number
  desktopImageId: number
}

export interface INewOrUpdatedAboutSubject {
  title: string
  description: string
  language: string
  visualElement: INewOrUpdatedVisualElement
}

export interface INewOrUpdatedFilmFrontPageData {
  name: string
  about: INewOrUpdatedAboutSubject[]
  movieThemes: INewOrUpdatedMovieTheme[]
  slideShow: string[]
  article?: string
}

export interface INewOrUpdatedMetaDescription {
  metaDescription: string
  language: string
}

export interface INewOrUpdatedMovieName {
  name: string
  language: string
}

export interface INewOrUpdatedMovieTheme {
  name: INewOrUpdatedMovieName[]
  movies: string[]
}

export interface INewOrUpdatedVisualElement {
  type: string
  id: string
  alt?: string
}

export interface INewSubjectFrontPageData {
  name: string
  externalId?: string
  banner: INewOrUpdateBannerImage
  about: INewOrUpdatedAboutSubject[]
  metaDescription: INewOrUpdatedMetaDescription[]
  editorsChoices?: string[]
  connectedTo?: string[]
  buildsOn?: string[]
  leadsTo?: string[]
}

export interface ISubjectPageData {
  id: number
  name: string
  banner: IBannerImage
  about?: IAboutSubject
  metaDescription?: string
  editorsChoices: string[]
  supportedLanguages: string[]
  connectedTo: string[]
  buildsOn: string[]
  leadsTo: string[]
}

export interface IUpdatedSubjectFrontPageData {
  name?: string
  externalId?: string
  banner?: INewOrUpdateBannerImage
  about?: INewOrUpdatedAboutSubject[]
  metaDescription?: INewOrUpdatedMetaDescription[]
  editorsChoices?: string[]
  connectedTo?: string[]
  buildsOn?: string[]
  leadsTo?: string[]
}

export interface IVisualElement {
  type: string
  url: string
  alt?: string
}
