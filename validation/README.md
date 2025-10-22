# Validation Module

## Overview
- Centralized HTML, MathML, and slug validation logic used by NDLA services to ensure content complies with editorial rules.
- Consumed by API modules whenever rich text or embed content is persisted.

## Key Components
- `ValidationRules.scala`: loads JSON rule sets (`embed-tag-rules.json`, `html-rules.json`, `mathml-rules.json`) from resources and exposes them as strongly typed models.
- `TagValidator.scala`: core engine that validates HTML/Embed tags against configured attribute rules, including resource-type-specific constraints and parent-child relationships.
- `HtmlTagRules.scala` & `TagRules.scala`: rule definitions and helper functions for checking allowed tags/attributes.
- `EmbedTagRules.scala`: specialized constraints for NDLA `<ndla-embed>` tags based on resource type.
- `SlugValidator.scala`: ensures slugs conform to naming conventions.
- `TextValidator.scala`: general-purpose length/emptiness validation utilities.
- `Exceptions.scala`: domain-specific validation exception hierarchy to align with controller error handling.
- `model/*`: typed representations of rule JSON payloads.

## Usage Guidelines
- Inject validators into services (e.g., `ContentValidator` implementations in API modules) to enforce rules before persisting or indexing content.
- When updating rule files, keep corresponding validator logic synchronized to avoid accidental false positives/negatives.
- Leverage `ValidationRules` helper methods to load compiled rule sets instead of parsing resource files manually.

## Testing
- Run `./mill validation.test` to ensure rule changes and validator logic remain consistent.

