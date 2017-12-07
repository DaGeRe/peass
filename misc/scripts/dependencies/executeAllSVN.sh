#!/bin/bash

export repos="http://svn.apache.org/repos/asf/commons/proper/chain/trunk/ commons-chain
http://svn.apache.org/repos/asf/commons/proper/codec/trunk commons-codec
http://svn.apache.org/repos/asf/commons/proper/collections/trunk commons-collections4
http://svn.apache.org/repos/asf/commons/proper/configuration/trunk commons-configuration2
http://svn.apache.org/repos/asf/commons/proper/daemon/trunk commons-daemon
http://svn.apache.org/repos/asf/commons/proper/dbutils/trunk commons-dbutils
http://svn.apache.org/repos/asf/commons/proper/digester/trunk commons-digester3-parent
http://svn.apache.org/repos/asf/commons/proper/discovery/trunk commons-discovery
http://svn.apache.org/repos/asf/commons/proper/email/trunk commons-email
http://svn.apache.org/repos/asf/commons/proper/exec/trunk commons-exec
http://svn.apache.org/repos/asf/commons/proper/functor/trunk/ commons-functor-parent
http://svn.apache.org/repos/asf/commons/proper/jci/trunk/ commons-jci
http://svn.apache.org/repos/asf/commons/proper/jcs/tags/commons-jcs-2.1 commons-jcs
http://svn.apache.org/viewvc/commons/proper/jelly/trunk/ jelly
http://svn.apache.org/repos/asf/commons/proper/jexl/tags/COMMONS_JEXL_3_1 commons-jexl3
http://svn.apache.org/repos/asf/commons/proper/jxpath/trunk commons-jxpath
http://svn.apache.org/repos/asf/commons/proper/launcher/trunk commons-launcher
http://svn.apache.org/repos/asf/commons/proper/logging/trunk commons-logging
http://svn.apache.org/repos/asf/commons/proper/net/tags/NET_3_6 commons-net
http://svn.apache.org/repos/asf/commons/proper/ognl/trunk/ commons-ognl
http://svn.apache.org/repos/asf/commons/proper/pool/trunk commons-pool2
http://svn.apache.org/repos/asf/commons/proper/proxy/trunk/ commons-proxy2-parent
http://svn.apache.org/repos/asf/commons/proper/validator/tags/VALIDATOR_1_6/ commons-validator
http://svn.apache.org/repos/asf/commons/proper/vfs/tags/commons-vfs2-project-2.1 commons-vfs2-project
http://svn.apache.org/repos/asf/commons/proper/weaver/trunk/ commons-weaver"

mkdir -p /home/diss/projekte

for repo in $repos; do
   # echo $repo
   if [[ ! $repo == "http://svn."* ]]; then
        echo $repo $last
        ./analyseSVNRepo.sh $last $repo
   fi
   last=$repo
done

