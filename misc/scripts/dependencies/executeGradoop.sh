#!/bin/bash

export repos="
https://github.com/dbs-leipzig/gradoop.git"

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

