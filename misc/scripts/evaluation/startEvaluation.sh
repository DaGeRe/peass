scriptdir=$(pwd)
cd ../../../evaluation/
mvn clean package
cd ../../
git clone https://github.com/infinitest/infinitest.git
mkdir projekte
cd infinitest
mvn clean install
cd $scriptdir
evaluateAllGit.sh
