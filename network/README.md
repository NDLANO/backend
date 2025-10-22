# Network Module

## Overview
- Shared networking stack for NDLA services, providing HTTP clients, Tapir controller scaffolding, authentication helpers, and Redis integration.
- Consumed by all API modules via the Mill dependency `build.network`.

## Core Packages
- `Tapir` (`tapir/*`):
  - `TapirController`, `TapirApplication`, and `Routes` wrap sttp Tapir server endpoints with NDLA defaults (JSON printer, error mapping, Swagger wiring, warm-up hooks).
  - `ControllerErrorHandling`, `ErrorHelpers`, and `TapirErrorHandling` centralize exception → HTTP response translation.
  - `auth/*` implements OAuth2 scope enforcement, Feide/MyNDLA token parsing, and combined user representations (`CombinedUser`, `TokenUser`).
- `clients/*`: typed REST clients using sttp for downstream NDLA services (`SearchApiClient`, `TaxonomyApiClient`, `FrontpageApiClient`, `MyNDLAApiClient`, `FeideApiClient`) plus `RedisClient` and `ScalaJedis` wrappers.
- `model/*`: shared DTOs for user identity, caching headers, HTTP error payloads, etc.
- `NdlaClient.scala`: utility for issuing authenticated HTTP calls with consistent logging and retry semantics.
- `jwt/*`: helpers for JWT parsing/validation used by auth flows.

## Usage Guidelines
- Extend `TapirController` in service modules to gain consistent prefix/tag handling, security helpers (`requirePermission`, `withOptionalMyNDLAUser`, etc.), and JSON serialization defaults.
- Acquire downstream clients from `ComponentRegistry` using implicit `given` bindings rather than constructing ad-hoc HTTP layers.
- Leverage `RedisClient` + `ScalaJedis` for caching and distributed locks instead of using the Jedis API directly.
- Reuse `ErrorHelpers` in controllers to map domain errors to standardized NDLA error payloads.

## Testing
- Run `./mill network.test` to execute the shared networking test suite.
- Combine with `tapirtesting` utilities for end-to-end controller testing in API modules.

