package de.peass.utils;

import java.io.PrintStream;

import de.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterSearchCause extends RunCommandWriter {

   public RunCommandWriterSearchCause(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super(goal, experimentId, dependencies);
      // TODO Auto-generated constructor stub
   }
   
   @Override
   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      goal.println("java -cp target/measurement-0.1-SNAPSHOT.jar de.peass.SearchChangeCause "
            + "-test " + testcaseName + " "
            + "-warmup 0 "
            + "-iterations 1000 "
            + "-repetitions 100 "
            + "-vms 100 "
            + "-timeout 10 "
            + "-type1error 0.1 "
            + "-type2error 0.1 "
            + "-version " + endversion + " "
            + "-executionfile $PEASS_REPOS/dependencies-final/execute_" + name + ".json "
            + "-folder ../../projekte/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + name + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");
   }

}
