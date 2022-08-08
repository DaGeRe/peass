#!/bin/bash

#SBATCH --nodes=1
#SBATCH --exclusive

export MY_HOME=/home/sc.uni-leipzig.de/do820mize

export JAVA_HOME=/usr/lib/jvm/java-1.8.0/
export PATH=$MY_HOME/maven/apache-maven-3.8.3/bin:/usr/lib/jvm/java-1.8.0/bin/:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:$MY_HOME/pxz:$MY_HOME/tar-1.29/bin/bin:$MY_HOME/git/git-2.9.5/bin-wrappers
export MAVEN_OPTS=""
#export MAVEN_OPTS="-Dmaven.repo.local=/nfs/user/do820mize/.m2/"

echo "Version Index: $INDEX Test: $TEST"
echo "Job Id: $SLURM_JOB_ID"
echo "Arrayindex: $SLURM_ARRAY_TASK_ID"
echo "Path: $PATH"
echo "MAVEN_OPTS: $MAVEN_OPTS"
echo "Projekt: $PROJECT"
echo "Home: $HOME"

mkdir $MY_HOME/.m2
cat <<MYFILE > $MY_HOME/.m2/settings.xml
<settings>
 <proxies>
   <proxy>
       <id>myproxy</id>
       <active>true</active>
       <protocol>http</protocol>
       <host>proxy.uni-leipzig.de</host>
       <port>3128</port>
   </proxy>
 </proxies>
</settings>
MYFILE


if [ "$PROJECT" == "https://github.com/eclipse/jetty.project.git" ]; then
  GOAL="jetty"
else
  GIT=$(echo $PROJECT | awk -F '/' '{print $NF}')
  GOAL=${GIT::-4}
fi

if [ -z $EXPERIMENT_ID ]
then
	EXPERIMENT_ID="undetermined_id"
fi

echo "Goal: $GOAL"

GOAL_START=$MY_HOME/peass_results/$GOAL/$EXPERIMENT_ID
mkdir $GOAL_START

JOB_FOLDER=$HOME/job_$INDEX\_$SLURM_JOB_ID
PROJECT_FOLDER=$(mktemp -d /tmp/XXXX)
DOKU_FOLDER=$JOB_FOLDER/logs
RESULT_FOLDER=$JOB_FOLDER/results
GOAL_FOLDER=$GOAL_START/$INDEX\_$SLURM_JOB_ID
echo "Ordner: $JOB_FOLDER Projektordner: $PROJECT_FOLDER"
rm -rf $PROJECT_FOLDER
mkdir $JOB_FOLDER
mkdir $PROJECT_FOLDER
mkdir $DOKU_FOLDER
echo "http_proxy: $http_proxy"
echo "Running git clone $PROJECT $PROJECT_FOLDER" 
srun --export=PATH,JAVA_HOME,https_proxy,http_proxy,HTTP_PROXY,HTTPS_PROXY -o $DOKU_FOLDER/clone_project git clone $PROJECT $PROJECT_FOLDER
ls -lah $PROJECT_FOLDER

export ITERATIONS=10
export REPETITIONS=1000000
export VMS=30
export USEKIEKER=false


PEASS_JAR=/home/sc.uni-leipzig.de/do820mize/peass/starter/target/peass-starter-*-SNAPSHOT.jar
echo "Starte Testausführung"
if [ ! -z "$TEST" ]; then
    srun --export=PATH,JAVA_HOME,MAVEN_OPTS -o $DOKU_FOLDER/test.out -l java -Xmx10G -cp $PEASS_JAR de.dagere.peass.MeasureStarter \
 	-executionFile /home/sc.uni-leipzig.de/do820mize/pronto-results/traceTestSelection_"$GOAL".json \
-folder $PROJECT_FOLDER -vms $VMS -iterations $ITERATIONS -repetitions $REPETITIONS -startcommit $START -endcommit $END -test $TEST 
else
    echo "Error, test was not defined!"
    exit 1
fi

echo "Testausführung beendet"$(date)
mv $JOB_FOLDER $GOAL_FOLDER
echo "Running mv "$PROJECT_FOLDER"_peass $GOAL_FOLDER/peass"
mv $PROJECT_FOLDER"_peass" $GOAL_FOLDER/peass
INDEX=$(basename $GOAL_FOLDER)
echo "Running tar --directory $GOAL_FOLDER/.. -cf "$GOAL_FOLDER".tar $INDEX/logs $INDEX/peass/logs/ $INDEX/peass/measurementsFull/"
srun --export=PATH tar --directory $GOAL_FOLDER/.. -cf "$GOAL_FOLDER".tar $INDEX/logs $INDEX/peass/logs/ $INDEX/peass/measurementsFull/

