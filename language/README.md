# Language Module

## Overview
- Shared library that models ISO language, script, and country metadata used across NDLA services for localization, validation, and presentation.
- Provides efficient lookups for ISO-639 (1/2/3), ISO-3166 country codes, and ISO-15924 scripts, plus helper traits for language-tagged domain objects.

## Key Components
- `Language.scala`: high-level API exposing `Language.AllLanguages`, normalization helpers, and conversion utilities.
- `model/Iso639List_*.scala`: generated case objects enumerating ISO-639 language codes grouped for faster lookup.
- `model/Iso3166.scala` / `model/Iso15924.scala`: enumerations for country and script codes.
- `model/LanguageField.scala`, `WithLanguage`, `WithLanguageAndValue`: shared domain traits for multi-lingual data structures.
- `model/LanguageNotSupportedException.scala`: error type thrown when encountering unsupported codes.

## Usage Guidelines
- Depend on `build.language` in Mill to gain access to all language utilities.
- Use `Language.parse` and the helper traits instead of storing raw strings when modelling localized content; this keeps validation consistent with API modules.
- Combine with validators from the `validation` module to enforce allowed locales per resource type.

## Testing
- Run `./mill language.test` to validate changes to the code lists or helper logic.

