# Common Module

## Overview
- Shared Scala 3 utilities consumed by all NDLA backend services for configuration, logging, error handling, DTO conversion, caching, and AWS integrations.
- Designed as a pure library module (no Docker image) with Mill configuration in `package.mill` that other modules depend on.

## Provided Building Blocks
- **Configuration & Environment**: `configuration/*` supplies the `BaseProps` abstraction, `Prop` descriptors, `Environment` helpers (auto-loading `.env` files), and `Constants`.
- **Runtime Infrastructure**: `Clock`, `Warmup`, `CorrelationID`, `UUIDUtil`, and `RequestLogger` provide consistent timing, tracing, and diagnostic utilities.
- **Error Handling**: `errors/*` defines NDLA-specific exceptions and `ExceptionLogHandler` wrappers used by all entrypoints.
- **JSON & Schema Support**: `CirceUtil`, `SchemaImplicits`, and `DeriveHelpers` standardize Circe encoders/decoders and Tapir schema derivation.
- **Caching**: `caching/Memoize.scala` implements memoization primitives (Redis-aware via higher modules) for expensive computations.
- **External Clients**: `aws/*` and `brightcove/*` wrap AWS SDK v2 (S3, Transcribe) and Brightcove APIs with NDLA defaults (`NdlaS3Client`, `NdlaAWSTranscribeClient`, `NdlaBrightcoveClient`).
- **Conversion Helpers**: `converter/CommonConverter.scala` collects cross-module mapping logic (e.g., DTO ↔ domain conversion).
- **Domain Models**: `model/*` exposes shared NDLA data types (dates, content URIs, etc.) to keep serialization consistent.

## Usage Guidelines
- Extend `Environment.setPropsFromEnv` in service main classes to hydrate `Props` subclasses backed by this module.
- Wrap main entrypoints with `ExceptionLogHandler.default` to guarantee structured logging on uncaught exceptions.
- Reuse the memoization helpers together with `RedisClient` from the `network` module instead of reimplementing caches.
- Place shared JSON schemas or DTO derivations here to avoid circular dependencies across API modules.

## Testing
- Run `./mill common.test` to execute the shared unit tests. Downstream services inherit these utilities, so changes here should be regression-tested jointly via the consuming modules.

