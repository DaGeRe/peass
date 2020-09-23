package de.peass.utils;

import java.io.PrintStream;

import de.peass.RootCauseAnalysis;
import de.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterSearchCause extends RunCommandWriter {

   public RunCommandWriterSearchCause(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super(goal, experimentId, dependencies);
   }

   @Override
   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      goal.println("java -cp distribution/target/peass-distribution-0.1-SNAPSHOT.jar " + RootCauseAnalysis.class.getCanonicalName() + " "
            + "-test " + testcaseName + " "
            + "-warmup 1000 "
            + "-iterations 1000 "
            + "-repetitions 100 "
            + "-vms 100 "
            + "-timeout 10 "
            + "-type1error 0.2 "
            + "-type2error 0.1 "
            + "-version " + endversion + " "
            + "-executionfile $PEASS_REPOS/dependencies-final/execute_" + name + ".json "
            + "-folder ../../projekte/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + name + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName, int warmup, int iterations, int repetitions, int vms) {
      goal.println("java -cp distribution/target/peass-distribution-0.1-SNAPSHOT.jar " + RootCauseAnalysis.class.getCanonicalName() + " "
            + "-measureComplete "
            + "-test " + testcaseName + " "
            + "-warmup " + warmup + " "
            + "-iterations " + iterations + " "
            + "-repetitions " + repetitions + " "
            + "-vms " + vms + " "
            + "-timeout 10 "
            + "-type1error 0.2 "
            + "-type2error 0.1 "
            + "-version " + endversion + " "
            + "-executionfile $PEASS_REPOS/dependencies-final/execute_" + name + ".json "
            + "-folder ../../projekte/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + name + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");
   }

}
