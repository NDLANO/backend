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

sbt -Ddocker.tag=$VERSION "set Test / test := {}" $SUBPROJECT/docker
echo "BUILT $PROJECT:$VERSION"
