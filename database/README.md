# Database Module

## Overview
- Shared database abstractions built on ScalikeJDBC and Flyway, providing reusable helpers for NDLA services that persist to PostgreSQL.
- Consumed by all API modules through Mill dependency `build.database`.

## Key Components
- `DataSource.scala`: lazily constructs the global HikariCP-backed connection pool and exposes accessors for services.
- `DBUtility.scala`: convenience functions for transactional execution, chunked queries, and row mapping.
- `DBMigrator.scala`: thin wrapper around Flyway that executes module-specific migrations declared in each service `ComponentRegistry`.
- `TableMigration.scala` & `DocumentMigration.scala`: base classes for structured migration steps with dependency handling.
- `LanguageFieldMigration.scala`: helper for migrating multilingual fields across tables.
- `MigrationUtility.scala`: utility methods used by concrete migration implementations (e.g., iterating large result sets safely).
- `DatabaseProps.scala`: base property definitions for database configuration keys.

## Usage Guidelines
- Instantiate `DBMigrator` with concrete migration classes inside each API module's `ComponentRegistry`. Migrations can compose `TableMigration` or `DocumentMigration` depending on DB structure.
- Use `DBUtility.localTx` / `readOnly` helpers instead of handling JDBC connections directly.
- Keep migrations idempotent; leverage `MigrationUtility` for batching to avoid long-running transactions.

## Testing
- Run `./mill database.test` to execute unit tests against the database helpers. Integration tests in API modules rely on this behavior, so verify compatibility when making changes.

