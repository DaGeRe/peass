package de.dagere.peass.utils;

import java.io.PrintStream;
import java.util.Set;

import de.peass.dependency.analysis.data.TestCase;
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
      if (dependencies.getUrl() == null) {
         throw new RuntimeException("Run commands can only be created if URL for download is present!");
      }
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
   
   public void createFullVersionCommand(int versionIndex, final String endversion, Set<TestCase> tests) {
      for (TestCase testcase : tests) {
         final String testcaseName = testcase.getClazz() + "#" + testcase.getMethod();
         createSingleMethodCommand(versionIndex, endversion, testcaseName);
      }
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      goal.println("java -cp target/measurement-0.1-SNAPSHOT.jar de.peass.AdaptiveTestStarter "
            + "-test " + testcaseName + " "
            + "-warmup 0 "
            + "-iterations 1000 "
            + "-repetitions 100 "
            + "-vms 100 "
            + "-timeout 10 "
//            + "-useGC false "
            + "-version " + endversion + " "
            + "-executionfile $PEASS_REPOS/dependencies-final/execute_" + name + ".json "
            + "-folder ../../projekte/" + name + "/ "
            + "-dependencyfile $PEASS_REPOS/dependencies-final/deps_" + name + ".json &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");
   }

}
