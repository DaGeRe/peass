PerAn
===================

PerAn (from Performance Analysation) is a tool to analyse the evolution of performance during various versions. Base of this analysis is the transformation of junit (3 and 4) tests to performance tests. Since performance measurements usually take a high count of repetitions and therefore time, first of all in the `dependency`-module, the tests that may have changed their performance are determined. Afterwards, the `measurement`-module allows to execute the measurement on given projects. This measurements can be analyzed using the `analysis`-module later.



# Dependency

The dependencies of a test are those classes or methods, that are called (transitively) by the test. So if the test itself or one of the dependencies is changed, the outcome of the test may be changed. The `dependency`-module creates the dependencies of every version of a project, in order to determine which test outcome may change in which version.

## Usage

Calling de.peran.DependencyReadingStarter -folder $FOLDER -out $OUTPUT starts the creation of dependencies for the project in $FOLDER. This folder should contain a git or svn-project. Afterwards, a file $OUTPUT containing the dependencies is created. This file contains all testcases and their dependencies in the `<initialversion>`-tag. Afterwards, in `<versions>` for every version where something build-changing happened a `<version>` tag is present that contains the test that need to be called and the dependencies that have changed.

## Implementation

In order to get the dependencies of a test, all tests are converted to KoPeMe-tests using Kieker, because Kieker allows to generate traces and KoPeMe allows to save the relation between test-call and Kieker-Trace-Folder. Afterwards, the tests are executed for every version. By parsing the sources (using javaparser) and the version-control-system-diffs, it is determined where changes have taken place.

## Shortcomings

During the dependency analysis, only changes to .java-files are recorded. If resource files, e.g. a hibernate configuration, changes, the change is not recorded. Furthermore the basic assumption is that call trees are deterministic and only change due to .java-file changes. For example in

```java
int index = new Random().nextInt();
Method method = method = obj.getClass().getMethod("myTest"+index);
method.invoke();
```

or

```java
int index = new Random().nextInt();
if (index == 2)
  myTest2();
else if (index == 3)
  myTest3();
```

only one of the dependencies (the one randomly chosen during the execution) would be regarded as a dependency. In this examples only `myTest2` or `myTest3` would be recored as dependency.

# Measurement

This module allows to measure the performance of every unit test in every revision of a project. 

This module is not published yet.

## Usage

TBD

## Implementation

TBD

# Analysis

This module allows to analyse performance measurements, i.e. compare them using statistical methods and analyse their traces in order to get hints what reasons performance changes could have.

This module is not published yet. 

## Usage

TBD

## Implementation

TBD
