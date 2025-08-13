# audio-api

![CI](https://github.com/NDLANO/audio-api/workflows/CI/badge.svg)

## Usage

API for accessing audio from NDLA. Adds, lists and/or returns an `Audio` file with metadata. Implements Elasticsearch for search within the audio database.

To interact with the api, you need valid security credentials.
To write data to the api, you need write role access.

### Avaliable Endpoints

- `GET /audio-api/v1/audio/` - Fetch a json-object containing a *list* with *all audio files available*.
- `GET /audio-api/v1/audio/<id>` - Fetch a json-object containing the *audio id* of the *audio file* that needs to be fecthed.
- `POST /audio-api/v1/audio/` - Upload a *new audio file* provided with metadata.
- `PUT /audio-api/v1/audio/<id>` - Update the *audio file* provided with metadata.
- `GET /audio-api/v1/series/` - Fetch a json-object containing a *list* with *all podcast series available*.
- `GET /audio-api/v1/series/<id>` - Fetch a json-object containing the *series id* of the *podcast series* that needs to be fecthed.
- `POST /audio-api/v1/series/` - Upload a *new podcast series* provided with metadata.
- `PUT /audio-api/v1/series/<id>` - Update the *podcast series* provided with metadata.

For a more detailed documentation of the API, please refer to the [API documentation](https://api.ndla.no) (Staging: [API documentation](https://staging.api.ndla.no)).
