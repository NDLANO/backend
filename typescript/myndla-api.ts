// DO NOT EDIT: generated file by scala-tsi

export interface IBreadcrumb {
  id: string
  name: string
}

export interface IConfigMeta {
  key: string
  value: (boolean | string[])
  updatedAt: string
  updatedBy: string
}

export interface IConfigMetaRestricted {
  key: string
  value: (boolean | string[])
}

export interface IFolder {
  id: string
  name: string
  status: string
  parentId?: string
  breadcrumbs: IBreadcrumb[]
  subfolders: IFolderData[]
  resources: IResource[]
  rank?: number
  created: string
  updated: string
  shared?: string
  description?: string
  owner?: IOwner
}

export type IFolderData = IFolder

export interface IMyNDLAGroup {
  id: string
  displayName: string
  isPrimarySchool: boolean
  parentId?: string
}

export interface IMyNDLAUser {
  id: number
  feideId: string
  username: string
  email: string
  displayName: string
  favoriteSubjects: string[]
  role: string
  organization: string
  groups: IMyNDLAGroup[]
  arenaEnabled: boolean
  shareName: boolean
}

export interface INewFolder {
  name: string
  parentId?: string
  status?: string
  description?: string
}

export interface INewResource {
  resourceType: string
  path: string
  tags?: string[]
  resourceId: string
}

export interface IOwner {
  name: string
}

export interface IResource {
  id: string
  resourceType: string
  path: string
  created: string
  tags: string[]
  resourceId: string
  rank?: number
}

export interface IUpdatedFolder {
  name?: string
  status?: string
  description?: string
}

export interface IUpdatedResource {
  tags?: string[]
  resourceId?: string
}
