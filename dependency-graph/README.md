# Dependency Graph Tool

## Overview
- Scala 3 command-line utility that scans the repository for Scala classes and reports cyclical dependencies between modules.
- Intended for local developer use to keep cross-module dependencies acyclic; invoked by running the Mill target `./mill dependency-graph.run`.

## How It Works
- `Main.scala` detects module directories by searching for `package.mill` files (excluding `modules` and `dependency-graph` itself).
- For each module, `ScalaFileParser` parses Scala sources using Scalameta and loads SemanticDB artefacts to extract symbol references.
- `CycleDetector.scala` constructs a directed graph of intra-module dependencies and prints cycles, while `Logger.scala` provides simple structured logging.
- Filters skip test, model, and Scala 2 compatibility sources (`filterFile` in `Main.scala`) to focus on production Scala 3 code paths.

## Usage
1. Ensure SemanticDB is generated (Mill compilation produces `.semanticdb` files automatically).
2. Run `./mill dependency-graph.run` from the repository root.
3. Inspect the logs to identify modules/classes that participate in cyclic references and refactor accordingly.

