PeASS
===================

PeASS (from Performance Analysis of Software System versions) is a tool to analyse the evolution of performance during various versions. Base of this analysis is the transformation of junit (3 and 4) tests to performance tests. Since performance measurements usually take a high count of repetitions and therefore time, first of all in the `dependency`-module, the tests that may have changed their performance are determined. Afterwards, the `measurement`-module allows to execute the measurement on given projects. This measurements can be analyzed using the `analysis`-module later.

All modules should be built with mvn clean package before execution.

# Dependency

The dependency-module makes it possible to determine which tests are likely to have changed performance based on static and and dynamic analysis of a projects sources. 

First, one determines whether test performance may have changed because a source that is called, a dependency, is changed. With a call to de.peran.DependencyReadingStarter -folder $PROJECTFOLDER the reading of dependencies is started for a checked-out-project. Therefore, all tests are converted to KoPeMe-tests using Kieker, because Kieker allows to generate traces and KoPeMe allows to save the relation between test-call and Kieker-Trace-Folder. Afterwards, they are executed for every version. By parsing the sources and the version-control-system-diffs, it is determined where changes have taken place. The generated dependencies are saved afterwards into results/ as XML-file.

Since those may contain dependencies that do not change performance, e.g. non-called added methods to called classes, in a second step it is possible to determine whether tests have changed based on their traces, i.e. the called methods, their order and their source. Therefore, call -dependencyfile $DEPENDENCYFILE -folder $PROJECTFOLDER. As a result, in results/ a JSON-file containing the tests where the source has changed is saved.

# Measurement

After determining the dependencies, tests should be executed. Testing can be manually started by 

java -jar target/measurement-0.1-SNAPSHOT.jar -folder .. -dependencyfile .. -executionfile .. -repetitions .. -vms .. -warmup .. -iterations .. -test ..

where all parameters should be filled in by the correct values. This starts, for every version each test which is marked in the executionfile for this version. The executionfile can be left out, then the executed tests are determined by the dependencyfile. Repetitions defines, how many times each test should be executed between two measurements, warmup defines the count of warmup executions and iterations defines how many measurement iterations (measurement start, repetition count execution, measurement stop) should be executed.

Since execution of tests normally takes much time, it is reasonable to start the tests on different computers. As an example, tests via slurm are enabled. Therefore, run

java -cp target/measurement-0.1-SNAPSHOT.jar de.peran.utils.DivideVersions -dependencyfile .. -executionfile .. > ../misc/scripts/slurm/runall.sh
chmod +x ../misc/scripts/slurm/runall.sh

in order to produce a list of calls, which is executable. Every call produces a slurm job executing one test. Afterwards switch to ../misc/scripts/slurm/ and run ./runall. It starts executeTests.sh on every cluster. If the count of warmup iterations, measurement iterations, repetitions or vms should be changed, edit executeTests.sh. 

# Analysis

Analysis enables determination of performance changes based on measurement values. Therefore, first copy all measurements (assuming your project is $PROJECT; those are in ../$PROJECT_peass/measurementsFull) from the measurement computers to your computer. If you executed the tests with slurm, those are all in /newnfs/user/do820mize/fertig/ and can be extracted with 

find . -name "*.tar" -exec tar --one-top-level -xvf {} \;

Afterwards, execute 

java -cp target/analysis-0.1-SNAPSHOT.jar -dependencyfile .. -data ..

This produces results/changes.json, which contains all changes, the diff-folder for the trace comparison and the possibility to add information to this change, e.g. a type. 

# Evaluation

Infinitest needs to be installed in the local maven repository. This can be done by calling ./startEvaluation in misc/scripts/evaluation. In order to evaluate the Apache Commons Repositories, call ./evaluateAllGit.sh. If only one defined repository $REPO needs to be called, call ./evaluteGitRepo.sh $REPO. 

Afterwards, the count of tests executed by executing all tests, only Infinitest-selected, ekstazi-selected or DePeC-selected tests can be determined. Assuming, that the DePeC-Results are stored in $DEPEC and the evaluation-results are stored in $EVALUATION, this can be done by running java -cp target/evaluation.jar de.peran.evaluation.CompareEvaluations $DEPEC $EVALUATION 

# Funding

The creation of this project was funded by Hanns-Seidel-Stiftung (https://www.hss.de/).
