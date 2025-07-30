#!/usr/bin/env sh
set -e

./mill mill.scalalib.scalafmt/reformatAll
./mill --meta-level=1 mill.scalalib.scalafmt/reformatAll
