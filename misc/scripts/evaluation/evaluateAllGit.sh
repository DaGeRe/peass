#!/bin/bash

export repos="
http://git-wip-us.apache.org/repos/asf/commons-compress.git
http://git-wip-us.apache.org/repos/asf/commons-crypto.git
http://git-wip-us.apache.org/repos/asf/commons-csv.git
http://git-wip-us.apache.org/repos/asf/commons-dbcp.git
http://git-wip-us.apache.org/repos/asf/commons-fileupload.git
http://git-wip-us.apache.org/repos/asf/commons-imaging.git
http://git-wip-us.apache.org/repos/asf/commons-io.git
http://git-wip-us.apache.org/repos/asf/commons-math.git
http://git-wip-us.apache.org/repos/asf/commons-numbers.git
http://git-wip-us.apache.org/repos/asf/commons-text.git"
#org.apache.commons.rng.internal.source64.SplitMix64Test
#http:////git-wip-us.apache.org/repos/asf/commons-rng.git ist raus wg. Speicherverbrauch
#http://git-wip-us.apache.org/repos/asf/commons-scxml.git ist raus wg. Speicherverbrauch
#http://git-wip-us.apache.org/repos/asf/commons-rdf.git -> Relativ schnell multimodul, deshalb raus
#http://git-wip-us.apache.org/repos/asf/commons-lang.git enthält nicht-utf8-Zeichen..

mkdir -p /home/diss/evaluation/projekte/ekstazi
mkdir -p /home/diss/evaluation/projekte/infinitest
mkdir -p /home/diss/evaluation/projekte/empty

for repo in $repos;  do 
  echo "Repo: $repo"
  ./evaluateGitRepo.sh $repo
done

