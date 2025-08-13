#!/usr/bin/env sh
set -e

./mill mill.scalalib.scalafmt/checkFormatAll
./mill --meta-level=1 mill.scalalib.scalafmt/checkFormatAll
