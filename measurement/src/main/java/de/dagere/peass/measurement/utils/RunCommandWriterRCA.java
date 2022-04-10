package de.dagere.peass.measurement.utils;

import java.io.PrintStream;

import de.dagere.peass.SearchCauseStarter;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.folders.ResultsFolders;

public class RunCommandWriterRCA extends RunCommandWriter {

   public RunCommandWriterRCA(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super(goal, experimentId, dependencies);
   }

   @Override
   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      createSingleMethodCommand(versionIndex, endversion, testcaseName, 1000, 10000, 10000, 100);
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName, final int warmup, final int iterations, final int repetitions,
         final int vms) {
      /*goal.println("java -cp distribution/target/peass-distribution-0.1-SNAPSHOT.jar " + SearchCauseStarter.class.getCanonicalName() + " "
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
            + "-executionfile $PEASS_REPOS/dependencies-final/" + ResultsFolders.TRACE_SELECTION_PREFIX + name + ".json "
            + "-folder ../projects/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/" + ResultsFolders.STATIC_SELECTION_PREFIX + name + ".json &> measurement_" + endversion.substring(0, 6) + "_"
            + testcaseName
            + ".txt");*/
      goal.println("./peasstomcat searchcauseParameterized "
              + endversion + " "
              + testcaseName);
   }

}
