package de.peass.utils;

import java.io.PrintStream;

import de.peass.RootCauseAnalysis;
import de.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterRCA extends RunCommandWriter {

   public RunCommandWriterRCA(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super(goal, experimentId, dependencies);
   }

   @Override
   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      createSingleMethodCommand(versionIndex, endversion, testcaseName, 1000, 10000, 10000, 100);
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName, int warmup, int iterations, int repetitions, int vms) {
      goal.println("java -cp distribution/target/peass-distribution-0.1-SNAPSHOT.jar " + RootCauseAnalysis.class.getCanonicalName() + " "
            + "-rcaStrategy COMPLETE "
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
            + "-folder ../projects/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + name + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");
   }

}
