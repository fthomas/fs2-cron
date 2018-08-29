#!/bin/sh

VERSION="$1"
if [ -z "$VERSION" ]; then
    exit 1
fi

cat << EOF > latestVersion.sbt
latestVersion in ThisBuild := "$VERSION"
EOF

sbt readme/tut

git commit -a -m "Setting version $VERSION"
git tag -a -s v$VERSION -m "Releasing $VERSION"
git push --tags
