#!/bin/bash

#SBATCH --nodes=1
#SBATCH --exclusive

export JAVA_HOME=/usr/jdk64/jdk1.8.0_77/
export PATH=/newnfs/user/do820mize/maven/apache-maven-3.5.0/bin:/usr/jdk64/jdk1.8.0_77/bin/:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/newnfs/user/do820mize/pxz:/newnfs/user/do820mize/tar-1.29/bin/bin
export MAVEN_OPTS="-Dmaven.repo.local=/newnfs/user/do820mize/.m2/"

echo "Version Index: $INDEX"
echo "Job Id: $SLURM_JOB_ID"
echo "Arrayindex: $SLURM_ARRAY_TASK_ID"
echo "Path: $PATH"
echo "MAVEN_OPTS: $MAVEN_OPTS"
echo "Projekt: $PROJECT"
echo "Home: $HOME"
#srun --export=PATH,JAVA_HOME,MAVEN_OPTS -o /newnfs/user/do820mize/test_path.out echo $MAVEN_OPTS
#srun -o /newnfs/user/do820mize/test_java.out java
#srun --export=PATH,JAVA_HOME,MAVEN_OPTS -o /newnfs/user/do820mize/build_kopeme.out --chdir=/newnfs/user/do820mize/KoPeMe/ mvn clean install -DskipTests=true


JOB_FOLDER=$HOME/job_$INDEX\_$SLURM_JOB_ID
PROJECT_FOLDER=$JOB_FOLDER/projekt/
DOKU_FOLDER=$JOB_FOLDER/logs
RESULT_FOLDER=$JOB_FOLDER/results
GOAL_FOLDER=/newnfs/user/do820mize/fertig/$INDEX\_$SLURM_JOB_ID
echo "Ordner: $JOB_FOLDER"
rm -rf $PROJECT_FOLDER
mkdir $JOB_FOLDER
mkdir $PROJECT_FOLDER
mkdir $DOKU_FOLDER
git clone $PROJECT $PROJECT_FOLDER
echo "Starte Testausführung"
#srun --export=PATH,JAVA_HOME,MAVEN_OPTS -o $DOKU_FOLDER/test.out -l java -Xmx10G -cp /newnfs/user/do820mize/performanzanalysator/measurement/target/measurement-0.1-SNAPSHOT.jar de.peran.DependencyTestPairStarter -dependencyfile /newnfs/user/do820mize/performanzanalysator/dependency/dependencies_cio5.xml -executionfile /newnfs/user/do820mize/performanzanalysator/execute.json -folder $PROJECT_FOLDER -iterationen 10 -warmup 0 -vms 1 -repetitions 200 -startversion $START -endversion $END
srun --export=PATH,JAVA_HOME,MAVEN_OPTS -o $DOKU_FOLDER/test.out -l java -Xmx10G -cp /newnfs/user/do820mize/performanzanalysator/measurement/target/measurement-0.1-SNAPSHOT.jar de.peran.DependencyTestPairStarter -dependencyfile /newnfs/user/do820mize/performanzanalysator/dependency/dependencies_cio5.xml -executionfile /newnfs/user/do820mize/performanzanalysator/execute.json -folder $PROJECT_FOLDER -iterationen 10000 -warmup 0 -vms 15 -repetitions 200 -startversion $START -endversion $END
mv $JOB_FOLDER $GOAL_FOLDER
echo "Running srun -C $GOAL_FOLDER -I pxz -cf "$GOAL_FOLDER".tar $GOAL_FOLDER/logs $GOAL_FOLDER/projekt_peass/measurementsFull/"
srun --export=PATH tar -C $GOAL_FOLDER -I pxz -cf "$GOAL_FOLDER".tar "$GOAL_FOLDER"/logs "$GOAL_FOLDER"/projekt_peass/measurementsFull/
