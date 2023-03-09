// DO NOT EDIT: generated file by scala-tsi

export type Error = (IForbiddenError | IBadRequestError | IUnprocessableEntityError | IUnauthorizedError | IGenericError | INotFoundError)

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

export interface IBadRequestError {
  code: string
  description: string
  occuredAt: string
  type: "BadRequestError"
}

export interface IBannerImage {
  mobileUrl?: string
  mobileId?: number
  desktopUrl: string
  desktopId: number
}

export interface IFilmFrontPageData {
  name: string
  about: IAboutFilmSubject[]
  movieThemes: IMovieTheme[]
  slideShow: string[]
}

export interface IForbiddenError {
  code: string
  description: string
  occuredAt: string
  type: "ForbiddenError"
}

export interface IFrontPageData {
  topical: string[]
  categories: ISubjectCollection[]
}

export interface IGenericError {
  code: string
  description: string
  occuredAt: string
  type: "GenericError"
}

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
  filters?: string[]
  externalId?: string
  layout: string
  twitter?: string
  facebook?: string
  banner: INewOrUpdateBannerImage
  about: INewOrUpdatedAboutSubject[]
  metaDescription: INewOrUpdatedMetaDescription[]
  topical?: string
  mostRead?: string[]
  editorsChoices?: string[]
  latestContent?: string[]
  goTo?: string[]
}

export interface INotFoundError {
  code: string
  description: string
  occuredAt: string
  type: "NotFoundError"
}

export interface ISubjectCollection {
  name: string
  subjects: ISubjectFilters[]
}

export interface ISubjectFilters {
  id: string
  filters: string[]
}

export interface ISubjectPageData {
  id: number
  name: string
  filters?: string[]
  layout: string
  twitter?: string
  facebook?: string
  banner: IBannerImage
  about?: IAboutSubject
  metaDescription?: string
  topical?: string
  mostRead: string[]
  editorsChoices: string[]
  latestContent?: string[]
  goTo: string[]
  supportedLanguages: string[]
}

export interface IUnauthorizedError {
  code: string
  description: string
  occuredAt: string
  type: "UnauthorizedError"
}

export interface IUnprocessableEntityError {
  code: string
  description: string
  occuredAt: string
  type: "UnprocessableEntityError"
}

export interface IUpdatedSubjectFrontPageData {
  name?: string
  filters?: string[]
  externalId?: string
  layout?: string
  twitter?: string
  facebook?: string
  banner?: INewOrUpdateBannerImage
  about?: INewOrUpdatedAboutSubject[]
  metaDescription?: INewOrUpdatedMetaDescription[]
  topical?: string
  mostRead?: string[]
  editorsChoices?: string[]
  latestContent?: string[]
  goTo?: string[]
}

export interface IVisualElement {
  type: string
  url: string
  alt?: string
}
