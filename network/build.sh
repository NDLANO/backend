#!/bin/bash
set -e

sbt '+ package'
sbt '+ test'