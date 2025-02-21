// DO NOT EDIT: generated file by scala-tsi

export type ConfigKey = ConfigKeyEnum

export enum ConfigKeyEnum {
  LEARNINGPATH_WRITE_RESTRICTED = "LEARNINGPATH_WRITE_RESTRICTED",
  MY_NDLA_WRITE_RESTRICTED = "MY_NDLA_WRITE_RESTRICTED",
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
  ADJECTIVE = "adjective",
  ADVERB = "adverb",
  CONJUNCTION = "conjunction",
  DETERMINER = "determiner",
  EXPRESSION = "expression",
  INTERJECTION = "interjection",
  NOUN = "noun",
  PREPOSITION = "preposition",
  PRONOUN = "pronoun",
  SUBORDINATING_CONJUNCTION = "subordinating-conjunction",
  VERB = "verb",
  AUXILIARY = "auxiliary",
  COMPLEMENT = "complement",
  COVERB = "coverb",
  DEMONSTRATIVE = "demonstrative",
  EXCLAMATION_WORD = "exclamation-word",
  LOCATION_WORD = "location-word",
  MEASURE_WORD = "measure-word",
  MARKER = "marker",
  MODAL_VERB = "modal-verb",
  NOUN_PHRASE = "noun-phrase",
  NOUN_ZH = "noun-zh",
  NUMERAL = "numeral",
  ONOMATOPOEIA = "onomatopoeia",
  PARTICLE = "particle",
  PERSONAL_PRONOUN = "personal-pronoun",
  PROPER_NOUN = "proper-noun",
  QUANTIFIER = "quantifier",
  QUESTION_WORD = "question-word",
  STATIVE_VERB = "stative-verb",
  SUFFIX = "suffix",
  TIME_WORD = "time-word",
  TIME_EXPRESSION = "time-expression",
  VERB_COMPLEMENT = "verb-complement",
  VERB_OBJECT = "verb-object",
}
