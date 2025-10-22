# Integration Tests Module

## Overview
- Aggregator Mill module that orchestrates cross-service integration tests. It does not ship runtime code; instead it depends on the compiled sources and tests of several API modules.

## Structure
- `package.mill` pulls in logging dependencies and, via the `test` sub-module, depends on:
  - `article-api`, `draft-api`, `learningpath-api`, `search-api` (and their respective test configurations)
  - Shared validation and ScalaTest utilities (`validation`, `validation.test`, `scalatestsuite`)
- Test sources live under `src/test`, ready for service-spanning suites (currently empty placeholders).

## Usage
- Execute `./mill integration-tests.test` to run all registered integration suites once they are implemented.
- Extend `src/test/scala` with scenarios that boot multiple services, exercise HTTP contracts across boundaries, and assert shared behaviors (e.g., search indexing, cross-service data flows).

