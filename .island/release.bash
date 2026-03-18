#!/bin/bash
version_file="version.txt"

# update the Maven version number
mvn versions:set -DnewVersion=$(sed 's/dev/SNAPSHOT/g' $version_file)

