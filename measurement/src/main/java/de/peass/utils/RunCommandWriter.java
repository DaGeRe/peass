package de.peass.utils;

import java.io.PrintStream;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.SelectedTests;

public class RunCommandWriter {
   protected final PrintStream goal;
   protected final String experimentId;
   // final Dependencies dependencies;
   protected String name;
   protected String url;
   protected int nice = 1000000;

   public RunCommandWriter(PrintStream goal, String experimentId, final SelectedTests dependencies) {
      super();
      this.goal = goal;
      this.experimentId = experimentId;
      name = dependencies.getName();
      url = dependencies.getUrl();
   }

   public RunCommandWriter(PrintStream goal, String experimentId, String name, String url) {
      super();
      this.goal = goal;
      this.experimentId = experimentId;
      this.name = name;
      this.url = url;
   }

   public void setNice(int nice) {
      this.nice = nice;
   }

   public void createFullVersionCommand(int versionIndex, final String endversion) {
      throw new RuntimeException("Not implemented yet.");
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      goal.println("java -cp target/measurement-0.1-SNAPSHOT.jar de.peass.AdaptiveTestStarter "
            + "-test " + testcaseName + " "
            + "-warmup 0 "
            + "-iterations 1000 "
            + "-repetitions 100 "
            + "-vms 100 "
            + "-timeout 10 "
            + "-version " + endversion + " "
            + "-executionfile $PEASS_REPOS/dependencies-final/execute_" + name + ".json "
            + "-folder ../../projekte/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + name + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");
   }

}
