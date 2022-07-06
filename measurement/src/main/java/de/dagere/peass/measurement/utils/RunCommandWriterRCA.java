package de.dagere.peass.measurement.utils;

import java.io.PrintStream;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.folders.ResultsFolders;

public class RunCommandWriterRCA extends RunCommandWriter {

   public RunCommandWriterRCA(MeasurementConfig config, final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super(config, goal, experimentId, dependencies);
   }

   @Override
   public void createSingleMethodCommand(final int versionIndex, final String commit, final String testcaseName) {
      createSingleMethodCommand(versionIndex, commit, testcaseName, 1000, 10000, 10000, 100);
   }

   public void createSingleMethodCommand(final int versionIndex, final String commit, final String testcaseName, final int warmup, final int iterations, final int repetitions,
         final int vms) {
      goal.println("./peass searchcause "
            + "-rcaStrategy UNTIL_SOURCE_CHANGE "
            + "-propertyFolder results/properties_" + name + " "
            + "-test " + testcaseName + " "
            + "-warmup " + warmup + " "
            + "-iterations " + iterations + " "
            + "-repetitions " + repetitions + " "
            + "-vms " + vms + " "
            + "-timeout 10 "
            + "-type1error 0.2 "
            + "-type2error 0.1 "
            + "-commit " + commit + " "
            + "-folder ../projects/" + name + "/ "
            + "-staticSelectionFile $PEASS_REPOS/dependencies-final/" + ResultsFolders.STATIC_SELECTION_PREFIX + name + ".json &> rca_" + commit.substring(0, 6) + "_"
            + testcaseName
            + ".txt");
   }

}
