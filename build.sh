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

./mill -Ddocker.tag=$VERSION $SUBPROJECT.docker.build
echo "BUILT $PROJECT:$VERSION"
