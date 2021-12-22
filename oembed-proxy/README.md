# oEmbed Proxy
![CI](https://github.com/NDLANO/oembed-proxy/workflows/CI/badge.svg)

## Usage
Proxy for making oEmbed requests to known oEmbed providers without running into CORS problems.
It implements the oEmbed specification as described at [oembed.com](http://oembed.com/).

It supports oEmbed from all providers listed on [oembed.com](http://oembed.com/#section7)

To interact with the API, you need valid security credentials; 
see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).

For a more detailed documentation of the API, please refer to the [API documentation](https://api.ndla.no) (Staging: [API documentation](https://staging.api.ndla.no)).

## Developer documentation

**Compile:** sbt compile

**Run tests:** sbt test

**Create Docker Image:** ./build.sh
