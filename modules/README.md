# Build Modules

## Overview
- Collection of Mill traits and helpers that define how NDLA backend modules are built, tested, containerized, and documented.
- Shared by every service through inheritance in each `package.mill`.

## Key Traits
- `BaseModule` (`package.mill`):
  - Extends Mill `ScalaModule` with standardized scalac options, resource directories, Scalafmt integration, GitHub workflow generation, copyright checks, and optional OpenAPI → TypeScript generation.
  - Exposes nested `TestBase` that configures ScalaTest, logging resources, and JVM options shared by test modules.
- `AutoEnvLoading.mill`: automatically loads environment variables from workspace-level `.env`, module-specific `.env`, and root `.env.<module>` files for Mill forked processes.
- `ScalacOptions.mill`: centralizes compiler switches (warnings, fatal errors on CI, inline limits) and test JVM flags.
- `OpenAPITSPlugin.mill`: adds `generateTypescript` command that runs the service with `--generate-openapi`, installs TypeScript dependencies (via the Yarn worker), and writes strongly typed TS exports.
- `DockerComponent.mill`: augments modules to build production Docker images with consistent base image, tags, environment defaults, and assembly filters.
- `GithubWorkflowPlugin.mill`: generates CI and release GitHub Actions workflows per module, computing affected paths from module dependencies.
- `CopyrightHeaderPlugin.mill`: provides `copyrightCheck`/`copyrightFix` tasks to enforce NDLA header templates.

## Shared Dependency Matrix
- `versions.mill` defines versions and groups for third-party dependencies (logging, Tapir, Elastic4s, AWS SDK, Flyway, etc.) and exposes curated sequences (`SharedDependencies`) consumed in each module’s `package.mill`.
- `Yarn` worker (in `package.mill`) orchestrates deterministic `yarn install` calls and TypeScript generation when OpenAPI schemas change.

## Usage Patterns
- New services should extend `BaseModule` (and `DockerComponent` if deployable) in their `package.mill` to inherit build behavior automatically.
- Regenerate GitHub workflows with `./mill _.ghGenerate`.
- Keep shared dependency versions in `versions.mill` synchronized to avoid drift between modules.

