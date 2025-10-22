# Tapir Testing Module

## Overview
- Shared test utilities for Tapir-based NDLA services. Provides fixtures, helpers, and assertions tailored to the shared networking stack.
- Configured as a non-component library in `package.mill`, depending on `common`, `network`, and `scalatestsuite`.

## Usage
- Import this module in service `test` dependencies (see `package.mill` in API modules) to gain access to:
  - Common Tapir endpoint builders and stubs
  - JSON serialization helpers consistent with production controllers
  - Elastic/Redis test fixtures via shared dependencies
- Extend test suites from the provided base classes to minimize boilerplate when exercising Tapir `ServerEndpoint`s.

## Running Tests
- `./mill tapirtesting.test` executes the module’s own tests (if present). Downstream modules pick up the utilities automatically through Mill dependency inheritance.

