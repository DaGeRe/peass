#!/bin/bash

export repos="
http://git-wip-us.apache.org/repos/asf/commons-io.git"

#org.apache.commons.rng.internal.source64.SplitMix64Test
#http:////git-wip-us.apache.org/repos/asf/commons-rng.git ist raus wg. Speicherverbrauch
#http://git-wip-us.apache.org/repos/asf/commons-scxml.git ist raus wg. Speicherverbrauch
#http://git-wip-us.apache.org/repos/asf/commons-rdf.git -> Relativ schnell multimodul, deshalb raus
#http://git-wip-us.apache.org/repos/asf/commons-lang.git enthält nicht-utf8-Zeichen..

PEASSHOME=$(pwd)/../../../

mkdir -p $PEASSHOME"projekte"

index=1
for repo in $repos;  do
  echo "Repo: $repo Index: $index"
  ./analyseGitRepo.sh $repo
  index=$((index+1))
  if [[ $# -eq 1 ]]; then
    if [[ $index -ge $1 ]]; then
      echo "Wird beendet"
      exit
    fi
  fi
done

