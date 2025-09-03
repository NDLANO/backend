# Dependency graph cycle lint

This module is a module to parse all the scala files in the repository to detect if we have any cycical dependencies across classes that are initialized in the `ComponentRegistry` files.

Ideally this should probably be a build-system (mill) plugin, but we will have to wait until [this](https://github.com/com-lihaoyi/mill/pull/5790) is released.

# Usage

```
# need to stand in the root of the project
cd $NDLA_HOME/backend

./mill _.semanticDbData
./mill dependency-graph.run
```
