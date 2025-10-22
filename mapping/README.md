# Mapping Module

## Overview
- Minimal shared library that normalizes NDLA-specific code mappings, currently focused on licenses and ISO-639 language metadata.
- Provides small, dependency-free helpers reused across API modules for consistent serialization and validation.

## Components
- `License.scala`: enumerates NDLA license types with helper methods for parsing, comparison, and rendering user-facing labels.
- `ISO639.scala`: utility functions for working with ISO-639 language codes alongside the richer structures in the `language` module.

## Usage
- Depend on `build.mapping` from Mill to access these helpers wherever license or language code normalization is required (e.g., controllers and converters).
- Extend this module when introducing new mapping tables to keep lightweight transformations centralized.

