# NDLA backend monorepo

This is a [sbt-multiproject](https://www.scala-sbt.org/1.x/docs/Multi-Project.html) repository for NDLA scala backend projects.

## Moving repositories

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
