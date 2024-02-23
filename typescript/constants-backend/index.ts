// DO NOT EDIT: generated file by scala-tsi

export type ConfigKey = ConfigKeyEnum

export enum ConfigKeyEnum {
  LEARNINGPATH_WRITE_RESTRICTED = "LEARNINGPATH_WRITE_RESTRICTED",
  MY_NDLA_WRITE_RESTRICTED = "MY_NDLA_WRITE_RESTRICTED",
  ARENA_ENABLED_ORGS = "ARENA_ENABLED_ORGS",
  ARENA_ENABLED_USERS = "ARENA_ENABLED_USERS",
  AI_ENABLED_ORGS = "AI_ENABLED_ORGS",
}

export type DraftStatus = DraftStatusEnum

export enum DraftStatusEnum {
  IMPORTED = "IMPORTED",
  PLANNED = "PLANNED",
  IN_PROGRESS = "IN_PROGRESS",
  EXTERNAL_REVIEW = "EXTERNAL_REVIEW",
  INTERNAL_REVIEW = "INTERNAL_REVIEW",
  QUALITY_ASSURANCE = "QUALITY_ASSURANCE",
  LANGUAGE = "LANGUAGE",
  FOR_APPROVAL = "FOR_APPROVAL",
  END_CONTROL = "END_CONTROL",
  PUBLISH_DELAYED = "PUBLISH_DELAYED",
  PUBLISHED = "PUBLISHED",
  REPUBLISH = "REPUBLISH",
  UNPUBLISHED = "UNPUBLISHED",
  ARCHIVED = "ARCHIVED",
}

export type Permission = PermissionEnum

export enum PermissionEnum {
  AUDIO_API_WRITE = "audio:write",
  ARTICLE_API_PUBLISH = "articles:publish",
  ARTICLE_API_WRITE = "articles:write",
  CONCEPT_API_ADMIN = "concept:admin",
  CONCEPT_API_WRITE = "concept:write",
  DRAFT_API_ADMIN = "drafts:admin",
  DRAFT_API_HTML = "drafts:html",
  DRAFT_API_PUBLISH = "drafts:publish",
  DRAFT_API_WRITE = "drafts:write",
  FRONTPAGE_API_ADMIN = "frontpage:admin",
  FRONTPAGE_API_WRITE = "frontpage:write",
  IMAGE_API_WRITE = "images:write",
  LEARNINGPATH_API_ADMIN = "learningpath:admin",
  LEARNINGPATH_API_PUBLISH = "learningpath:publish",
  LEARNINGPATH_API_WRITE = "learningpath:write",
}

export type WordClass = WordClassEnum

export enum WordClassEnum {
  adjective = "adjective",
  adverb = "adverb",
  conjunction = "conjunction",
  determiner = "determiner",
  expression = "expression",
  interjection = "interjection",
  noun = "noun",
  preposition = "preposition",
  pronoun = "pronoun",
  subordinating-conjunction = "subordinating-conjunction",
  verb = "verb",
  auxiliary = "auxiliary",
  complement = "complement",
  coverb = "coverb",
  demonstrative = "demonstrative",
  exclamation-word = "exclamation-word",
  location-word = "location-word",
  measure-word = "measure-word",
  marker = "marker",
  modal-verb = "modal-verb",
  noun-phrase = "noun-phrase",
  noun-zh = "noun-zh",
  numeral = "numeral",
  onomatopoeia = "onomatopoeia",
  particle = "particle",
  personal-pronoun = "personal-pronoun",
  proper-noun = "proper-noun",
  quantifier = "quantifier",
  question-word = "question-word",
  stative-verb = "stative-verb",
  suffix = "suffix",
  time-word = "time-word",
  time-expression = "time-expression",
  verb-complement = "verb-complement",
  verb-object = "verb-object",
}
