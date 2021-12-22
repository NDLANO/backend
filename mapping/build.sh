#!/bin/bash
set -e

sbt test && sbt package
