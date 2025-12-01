#!/bin/bash
set -e

SUBPROJECT="$1"
VERSION="$2"
NDLAComponentName=$SUBPROJECT

source ./build.properties
PROJECT="$NDLAOrganization/$NDLAComponentName"

if [ -z $SUBPROJECT ]
then
    echo "This build-script requires an argument for subproject to build."
    exit 1
fi

if [ -z $VERSION ]
then
    VERSION="SNAPSHOT"
fi

BUILD_CMD=${DOCKER_BUILD_CMD:-docker build}
BUILD_ARGS=${DOCKER_BUILD_ARGS:-}
BUILD_TAG_ARGS=${DOCKER_BUILD_TAG_ARGS:---tag $PROJECT:$VERSION}

$BUILD_CMD \
  $BUILD_ARGS \
  --build-arg MODULE=$SUBPROJECT \
  $BUILD_TAG_ARGS \
  .

echo "BUILT $PROJECT:$VERSION"
