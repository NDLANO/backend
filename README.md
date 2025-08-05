# NDLA backend monorepo

This is a [mill multi-file project](https://mill-build.org/mill/large/multi-file-builds.html) repository for NDLA scala backend projects.

This means this contains all scala backend components for the NDLA project.
There will be more detailed README's in the respective subdirectories.

## Developer documentation

**Compile subproject**: `./mill test-api.compile`

**Run tests:** `./mill test-api.test`

**Create Docker Image:** `./build.sh test-api`

**Check code formatting:** `./checkfmt.sh`

**Automatically format code files:** `./fmt.sh`

**Generate typescript files:** `mill test-api.generateTypescript`

You could run the tasks directly to execute the tasks for _all_ subprojects (IE: `./mill _.test`), this however can take a long time and in some cases even fail because of dependencies or jvm memory problems. We should improve upon this in the future, but for now it imposes no real problems.

### IntelliJ jvm options

When using IntelliJ it is useful to setup required [jvmoptions](.jvmopts) in templates for `scalatest` under
run/debug configurations.
