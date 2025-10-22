# Search Module

## Overview
- Shared ElasticSearch abstraction layer used by multiple NDLA services for indexing and querying content.
- Provides builders, converters, and error types around the Elastic4s `esjava` client.

## Key Components
- `Elastic4sClient.scala`: wraps client configuration (hosts, request logging, retry logic) and exposes typed methods for search/index operations.
- `NdlaE4sClient` (constructed via `Elastic4sClientFactory` in consuming modules) centralizes connection details and metrics.
- `BaseIndexService.scala`: base trait for building and executing index operations with unified error handling and bulk support.
- `SearchConverter.scala`: conversions between domain models and Elastic documents, including serialization helpers.
- `AggregationBuilder.scala` and `FakeAgg.scala`: utilities for constructing aggregations and faking responses in tests.
- `SearchLanguage.scala`: ensures consistent handling of locale-specific analyzers and fallback behavior.
- `IndexNotFoundException.scala`, `NdlaSearchException.scala`: typed exceptions thrown when Elastic operations fail.
- `TestUtility.scala`: helpers for standing up test Elastic clusters in module test suites.

## Usage Guidelines
- Import `Elastic4sClientFactory.getClient` from consuming modules’ component registries to obtain an `NdlaE4sClient`.
- Extend `BaseIndexService` in module-specific search/index services to reuse retry, batching, and error propagation logic.
- Reuse `SearchConverter` to translate domain entities into Elastic-compatible documents, keeping mappings consistent across modules.

## Testing
- Run `./mill search.test` for unit tests (includes optional Elastic Testcontainers support).
- Combine with `tapirtesting` to exercise search endpoints end-to-end from module test suites.

