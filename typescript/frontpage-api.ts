// DO NOT EDIT: generated file by scala-tsi

export type ErrorBody = (IBadGatewayBody | IGenericBody | IUnprocessableEntityBody | IBadRequestBody | IUnauthorizedBody | INotImplementedBody | INotFoundBody | IForbiddenBody)

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

export interface IBadGatewayBody {
  code: string
  description: string
  occurredAt: string
  type: "BadGatewayBody"
}

export interface IBadRequestBody {
  code: string
  description: string
  occurredAt: string
  type: "BadRequestBody"
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

export interface IForbiddenBody {
  code: string
  description: string
  occurredAt: string
  type: "ForbiddenBody"
}

export interface IFrontPageData {
  topical: string[]
  categories: ISubjectCollection[]
}

export interface IGenericBody {
  code: string
  description: string
  occurredAt: string
  type: "GenericBody"
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

export interface INotFoundBody {
  code: string
  description: string
  occurredAt: string
  type: "NotFoundBody"
}

export interface INotImplementedBody {
  code: string
  description: string
  occurredAt: string
  type: "NotImplementedBody"
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

export interface IUnauthorizedBody {
  code: string
  description: string
  occurredAt: string
  type: "UnauthorizedBody"
}

export interface IUnprocessableEntityBody {
  code: string
  description: string
  occurredAt: string
  type: "UnprocessableEntityBody"
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
