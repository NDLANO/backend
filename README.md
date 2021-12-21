# NDLA backend monorepo

This is a [sbt-multiproject](https://www.scala-sbt.org/1.x/docs/Multi-Project.html) repository for NDLA scala backend projects.

## Possible merge strategies

<details>
    <summary>With subtree</summary>

### How to merge

- Run the subtree command to add:
  - `git subtree add --prefix=test-api git@github.com:ndlano/test-api.git master`

### How to move back into standalone repo:

- Run the subtree command to push back:
  - `git subtree push --prefix=test-api git@github.com:ndlano/test-api.git master`

### Pros
- Easy to merge into the monorepo
- Easy to pull changes that happened after the initial merge
- Easy to push back changes and keeping the history separate

### Cons
- Viewing history before the merge is "difficult". You need to know the old path.
  - Ex: You want to view the history of `image-api/src/main/scala/no/ndla/imageapi/JettyLauncher.scala`
  - Normally you would do `git log --follow -- image-api/src/main/scala/no/ndla/imageapi/JettyLauncher.scala`, but since we used subtree (and the `git log --follow` implementation is not perfect) this doesn't work. We would have to know the historic path and use this instead `git log -- src/main/scala/no/ndla/imageapi/JettyLauncher.scala`.
  - This can be cumbersome, but more importantly it makes gui-tools to view history think the file was introduced at the `subtree add` command.

</details>

<details>
    <summary>Without subtree</summary>

### How to merge a repository into the backend repo:

- Add the remote: `git remote add test-api-remote git@github.com:ndlano/test-api-remote.git`
- Fetch remote `git fetch test-api-remote`
- Pull the remote into a separate branch: `git checkout -b test-api-branch test-api-remote/master`
- Move all files into the subdirectory that the repo will live in: `mkdir test-api && mv * test-api/`
- Commit this: `git add . && git commit -m "Move test-api into subdirectory"`
- Merge this into the master branch: `git checkout master && git merge test-api-branch`
- Great success!

### How to move a repository back into a standalone repo:

Hopefully this wont necessary, but _if_ we ever wanted to move back, we would just do the same process in reverse:

- Add the remote if it isn't already there: `git remote add test-api-remote git@github.com:ndlano/test-api-remote.git`
- Make a branch to work in so we don't break master of the monorepo: `git checkout -b test-api-remerge`
- Delete everything we don't want back in the single repo (ndlano/test-api) from the root path.
- Move the subdirectory back into root: `mv test-api/* .`
    - Delete subdir `rm -d test-api`
- Commit this: `git add . && git commit -m "Move test-api back to root"`
- Merge this back into the original repo: `git checkout -b test-api-branch test-api-remote/master && git merge test-api-remerge`
- IF everything seems okay this can now be pushed to the master of the standalone repository by simply doing `git push`

### Pros
- History is "perfect" as in the history of the files are complete and tools work as expected
### Cons
- Merging is a bit trickier, but doable
- Pushing back into the standalone repository is difficult without screwing up the history of the original repository with changes from all sub-projects.
- Pulling changes from the subprojects after the initial merge is a hassle
</details>
