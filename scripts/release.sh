#!/bin/sh

VERSION="$1"
if [ -z "$VERSION" ]; then
    exit 1
fi

cat << EOF > latestVersion.sbt
latestVersion in ThisBuild := "$VERSION"
EOF

sbt readme/mdoc

git commit -a -m "Setting version to $VERSION"
git tag -a -s v$VERSION -m "Releasing $VERSION"
git push
git push --tags
