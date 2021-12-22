# search-api
![CI](https://github.com/NDLANO/search-api/workflows/CI/badge.svg)

## Usage

API to search across multiple NDLA APIs

For a more detailed documentation of the API, please refer to the [API documentation](https://api.ndla.no) (Staging: [API documentation](https://staging.api.ndla.no)).


## Developer documentation

**Compile:** sbt compile

**Run tests:** sbt test

**Create Docker Image:** ./build.sh

**Check code formatting:** sbt checkfmt

**Automatically format code files:** sbt fmt

### Testing gotchas
You might get unexplainable test errors if you have set a timezone in sbt-settings. Try setting 'SBT_OPTS=-Duser.timezone=CET' in your terminal.
