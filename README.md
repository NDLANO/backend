# audio-api

[![Build Status](https://travis-ci.org/NDLANO/audio-api.svg?branch=master)](https://travis-ci.org/NDLANO/audio-api)

## Usage

API for accessing audio from NDLA. Adds, lists and/or returns an `Audio` file with metadata. Implements ElasticSearch for search within the audio database.

To interact with the api, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.

### Avaliable Endpoints

- `GET /audio-api/v1/audio/` - Fetch a json-object containing a *list* with *all audio files available*.
- `GET /audio-api/v1/audio/<id>` - Fetch a json-object containing the *audio id* of the *audio file* that needs to be fecthed.
- `POST /audio-api/v1/audio/` - Upload a *new audio file* provided with metadata.

For a more detailed documentation of the API, please refer to the [API documentation](https://staging.api.ndla.no).

## Developer documentation

**Compile:** sbt compile

**Run tests:** sbt test

**Create Docker Image:**./build.sh

### IntegrationTest Tag and sbt run problems

Tests that need a running elasticsearch outside of component, e.g. in your local docker are marked with selfdefined java
annotation test tag  ```IntegrationTag``` in ```/ndla/audio-api/src/test/java/no/ndla/tag/IntegrationTest.java```.
As of now we have no running elasticserach or tunnel to one on Travis and need to ignore these tests there or the build will fail.
Therefore we have the
 ```testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")``` in ```build.sbt```
