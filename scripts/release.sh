#!/bin/sh

VERSION="$1"
if [ -n "$VERSION" ]; then
    git tag -a -s v$VERSION -m "Releasing $VERSION"
    git push --tags
fi
