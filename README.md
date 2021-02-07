Peass
===================

Peass (from Performance analysis of software system versions) is a tool to analyse the evolution of performance during its versions. Base of this analysis is the transformation of JUnit (3 and 4 stable, 5 experimental) tests to performance tests. Since performance measurements need to be repeated often in order to produce statistically reliable results, they need much time. In order to reduce measurement time as far as possible, the regression test selection PRONTO (PeRfOrmance regressiON Test selectiOn) should be executed first. This is done in the `dependency`-module. Afterwards, the `measurement`-module supports execution of the measurements in the selected tests and versions. This measurements can be analyzed using the `analysis`-module later.

All modules should be built with `mvn clean package` before execution. In order to build peass, you'll need to install KoPeMe (https://github.com/DaGeRe/KoPeMe) first or have maven snapshot repo in your `settings.xml`.

In order get help, you can also just run `./peass`. If you need tab-completion in bash, run `. peass_completion` after `mvn install` is finished.

# Dependency

The dependency-module makes it possible to determine which tests may have changed performance based on static and and dynamic analysis of a projects sources. 

The easiest way to determine the changes tests is using `./peass select -folder $PROJECTFOLDER`. In order to parallelize the process, it is possible to further specify the count of parallel threads using `-threads ..` . 

This executes two steps: The static test selection and the trace analysis. These are described in the following. You should only need these for debugging or individual configurations.

## Static Test Selection

Static test selection determines whether a tests performance may have changed because a source that is called, a dependency, is changed. With a call to `de.peass.DependencyReadingStarter -folder $PROJECTFOLDER` the reading of dependencies is started for a checked-out-project. Therefore, all tests are converted to KoPeMe-tests using Kieker, because Kieker allows to generate traces and KoPeMe allows to save the relation between test-call and Kieker-Trace-Folder. Afterwards, they are executed for every version. By parsing the sources and the version-control-system-diffs, it is determined where changes have taken place. The generated dependencies are saved afterwards into results/ as JSON-file, the $DEPENDENCYFILE (which is named deps_$PROJECT.json).

## Trace Analysis

Since the static selected changes may contain dependencies that do not change performance, e.g. non-called added methods to called classes, it is possible to determine whether tests have changed based on their traces, i.e. the called methods, their order and their source. Therefore, call `de.peass.ViewPrintStarter -dependencyfile $DEPENDENCYFILE -folder $PROJECTFOLDER`. As a result, in results/ a JSON-file, the executionfile (which is named execute_$PROJECT.json), containing the tests where the source has changed is created.

## Evaluation

The selection rate of PRONTO can be evaluated against the selection rate of EKSTAZI and Infinitest using the pronto-evaluation project (http://github.com/dagere/pronto-evaluation).

# Measurement

After creation of the dependencyfile and/or the executionfile, tests can be executed. Testing can be manually started by 

`./peass measure -folder ..  -executionfile .. (-dependencyfile .. -repetitions .. -vms .. -warmup .. -iterations .. -test ..)`

where `folder`and `executionfile` or `dependencyfile` need to be set. All other parameters are optional.. This starts, for every version each test which is contained in the executionfile for this version (if it is the given test or there is no test given). The dependencyfile can be left out, then the executed tests are determined by the executionfile. Repetitions defines, how many times each test should be executed between two measurements, warmup defines the count of warmup executions and iterations defines how many measurement iterations (measurement start, repetition count execution, measurement stop) should be executed.

Since execution of tests normally takes much time, it is reasonable to start the tests on different computers. As an example, test may be distributed via slurm. Therefore, run

`./peass createScript de.peass.utils.DivideVersions -dependencyfile .. -executionfile .. -useSlu -useSlurm > ../misc/scripts/slurm/runall.sh`
`chmod +x ../misc/scripts/slurm/runall.sh`

in order to produce a list of calls, which is executable. Every call produces a slurm job executing one test. Afterwards switch to `../misc/scripts/slurm/` and run `./runall`. It starts `executeTests.sh` on every cluster. If the count of warmup iterations, measurement iterations, repetitions or vms should be changed, edit `executeTests.sh`. 

# Analysis

Analysis enables determination of performance changes based on measurement values. Therefore, two steps are executed: The cleanup and the determination of changes.

## Determination of changes

In order to get all changes, execute `./peass getchanges -dependencyfile $DEPENDENCYFILE -out $OUTFOLDER -data $DATAFOLDER`; where $DATAFOLDER should be the folder containing your cleaned data (normall `$PROJECTNAME_peass/clean`). Afterwards, two files are created:
- The changefile in `$OUTFOLDER`, containing all versions and test cases where measurement values changed based on agnostic t-test
- The statisticsfils in `$OUTFOLDER/statistics`, containing all versions and test cases, including the measurements with no changes

## Individual Cleanup

The resultfolder of your project is `$PROJECTNAME_peass`. In general, a clean/ folder is created in the resultfolder of your project. It contains measurementfiles only containing the statistical information about the second half of each VM start; the first half is seen as warmup. If you want to clean your data differently, e.g. remove different size of warmup, you can use the de.peass.TestCleaner. This is done by calling `./peass clean -dependencyfile $DEPENDENCYFILE -data $DATAFOLDER`, where `$DATAFOLDER` should contain all measurements.

# General Options

For dependency and measurement, by providing `-startversion` and/or `-endversion`, only the defined areas of the version history are analyzed / measured.

# Funding

The creation of this project was funded by a PhD scholarship of Hanns-Seidel-Stiftung (https://www.hss.de/).

# Citation

If you use Peass for research, please cite `Reichelt, David Georg, Stefan Kühne, and Wilhelm Hasselbring. "Peass: a tool for identifying performance changes at code level." 2019 34th IEEE/ACM International Conference on Automated Software Engineering (ASE). IEEE, 2019.`
