#!/usr/bin/env bash

set -exo pipefail

# Gets a property out of a .properties file
# usage: getProperty $key $filename
function getProperty() {
    grep "${1}" "$2" | cut -d'=' -f2
}

NEW_SNAPSHOT_VERSION=$1
CUR_SNAPSHOT_VERSION=$(getProperty 'VERSION_NAME' gradle.properties)

if [ -z "$NEW_SNAPSHOT_VERSION" ]; then
  # If no snapshot version was provided, use the current value
  NEW_SNAPSHOT_VERSION=$CUR_SNAPSHOT_VERSION
fi


echo "new_snapshot_version=$NEW_SNAPSHOT_VERSION" >> $GITHUB_OUTPUT
echo "cur_snapshot_version=$CUR_SNAPSHOT_VERSION" >> $GITHUB_OUTPUT
