PerAn
===================

PerAn (from Performance Analysation) is a tool to analyse the evolution of performance during various versions. Base of this analysis is the transformation of junit (3 and 4) tests to performance tests. Since performance measurements usually take a high count of repetitions and therefore time, first of all in the `dependency`-module, the tests that may have changed their performance are determined. Afterwards, the `measurement`-module allows to execute the measurement on given projects. This measurements can be analyzed using the `analysis`-module later.



# Hierarchy

With a call to de.peran.DependencyReadingStarter -folder .. the reading of dependencies is started. Therefore, all tests are converted to KoPeMe-tests using Kieker, because Kieker allows to generate traces and KoPeMe allows to save the relation between test-call and Kieker-Trace-Folder. Afterwards, they are executed for every version. By parsing the sources and the version-control-system-diffs, it is determined where changes have taken place. The generated dependencies are saved afterwards.

Details will follow.

# Measurement

This module is not published yet.

# Analysis

This module is not published yet. 
