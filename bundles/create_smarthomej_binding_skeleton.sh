#!/bin/bash

[ $# -lt 3 ] && { echo "Usage: $0 <BindingIdInCamelCase> <Author> <GitHub Username>"; exit 1; }

smartHomeJVersion=3.1.0-SNAPSHOT
openHABVersion=3.1.0-SNAPSHOT

camelcaseId=$1
id=`echo $camelcaseId | tr '[:upper:]' '[:lower:]'`

author=$2
githubUser=$3

mvn -s archetype-settings.xml archetype:generate -N \
  -DarchetypeGroupId=org.openhab.core.tools.archetypes \
  -DarchetypeArtifactId=org.openhab.core.tools.archetypes.binding \
  -DarchetypeVersion=$openHABVersion \
  -DgroupId=org.smarthomej.binding \
  -DartifactId=org.smarthomej.binding.$id \
  -Dpackage=org.smarthomej.binding.$id \
  -Dversion=$smartHomeJVersion \
  -DbindingId=$id \
  -DbindingIdCamelCase=$camelcaseId \
  -DvendorName="SmartHome/J" \
  -Dnamespace=org.smarthomej \
  -Dauthor="$author" \
  -DgithubUser="$githubUser"

directory="org.smarthomej.binding.$id/"

cp ../src/etc/NOTICE "$directory"

