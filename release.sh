#!/usr/bin/env bash

# Clone secret repo with resources to release to our environment
git clone --depth 1 https://knowit-at-ndla:$TRAVIS_RELEASE_GITHUB_TOKEN@github.com/ndlano/deploy.git

python3 -m pip install --upgrade pip
python3 -m pip install -r deploy/scripts/pyshare/requirements.txt
