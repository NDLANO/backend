# NDLA backend monorepo

This is a [sbt-multiproject](https://www.scala-sbt.org/1.x/docs/Multi-Project.html) repository for NDLA scala backend projects.

This means this contains all scala backend components for the NDLA project.
There will be more detailed README's in the respective subdirectories.


## Developer documentation
**Compile subproject**: `sbt test-api/compile`

**Run tests:** `sbt test-api/test`

**Create Docker Image:** `sbt test-api/docker`

**Check code formatting:** `sbt test-api/checkfmt`

**Automatically format code files:** `sbt test-api/fmt`

**Generate typescript files:** `sbt test-api/openapiTSGenerate`

You could run the sbt-tasks directly to execute the tasks for _all_ subprojects (IE: `sbt test`), this however can take a long time and in some cases even fail because of dependencies or jvm memory problems. We should improve upon this in the future, but for now it imposes no real problems.

**Check code formatting for everything:** `sbt scalafmtCheck scalafmtCheckSbt`

**Apply code formatting for everything:** `sbt scalafmt scalafmtSbt`

### IntelliJ jvm options

When using IntelliJ it is useful to setup required [jvmoptions](.jvmopts) in templates for `sbt task` and `scalatest` under 
run/debug configurations.


### Merging in sub-projects

When the sub-projects was merged in from the separate repositories we used the `git subtree` command.
This allows us to more easily maintain the separate repositories should we ever want to go back, or if we ever need to pull in changes from one of the separate repositories.

<details>
    <summary>Subtree merge strategy</summary>


### How to merge

- Run the subtree command to add:
  - `git subtree add --prefix=test-api git@github.com:ndlano/test-api.git master`

### How to pull changes in standalone repo:
- Run the subtree pull command:
  - `git subtree --prefix=test-api git@github.com:ndlano/test-api.git master`

### How to move back into standalone repo:

- Run the subtree command to push back:
  - `git subtree push --prefix=test-api git@github.com:ndlano/test-api.git master`

There can be some gotchas with viewing history from the standalone repositories in there, so we might consider merging the subprojects in directly (with `git merge --allow-unrelated-histories`) when can confirm this way of managing the backend projects is better.

</details>
