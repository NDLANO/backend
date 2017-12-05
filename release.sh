#!/bin/bash

sbt 'release cross' -Dnexus.host=$NEXUS_HOST
