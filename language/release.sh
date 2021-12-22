#!/bin/bash

sbt -Dnexus.host=$NEXUS_HOST 'release cross'
