# Testbase Module

## Overview
- Lightweight library that collects shared testing infrastructure (logging config, database helpers, ScalaTest traits) for NDLA backend modules.
- Extended by most service `test` sub-modules via `super.moduleDeps` in their Mill definitions.

## Usage
- Depend on `build.testbase` to inherit common ScalaTest configuration, Log4j settings, and convenience traits.
- Combine with `tapirtesting` and other module-specific fixtures when composing end-to-end suites.

## Running Tests
- Execute `./mill testbase.test` to validate any utility changes before rolling them out to consuming modules.

