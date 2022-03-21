package de.dagere.peass.measurement.utils;

import java.io.PrintStream;
import java.util.Set;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.folders.ResultsFolders;

public class RunCommandWriter {
   protected final PrintStream goal;
   protected final String experimentId;
   // final Dependencies dependencies;
   protected String name;
   protected String url;
   protected int nice = 1000000;

   public RunCommandWriter(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super();
      this.goal = goal;
      this.experimentId = experimentId;
      if (dependencies.getUrl() == null) {
         throw new RuntimeException("Run commands can only be created if URL for download is present!");
      }
      name = dependencies.getName();
      url = dependencies.getUrl();
   }

   public RunCommandWriter(final PrintStream goal, final String experimentId, final String name, final String url) {
      super();
      this.goal = goal;
      this.experimentId = experimentId;
      this.name = name;
      this.url = url;
   }

   public void setNice(final int nice) {
      this.nice = nice;
   }
   
   public void createFullVersionCommand(final int versionIndex, final String endversion, final Set<TestCase> tests) {
      for (TestCase testcase : tests) {
         final String testcaseName = testcase.getClazz() + "#" + testcase.getMethod();
         createSingleMethodCommand(versionIndex, endversion, testcaseName);
      }
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      /*goal.println("./peass measure "
            + "-test " + testcaseName + " "
            + "-warmup 0 "
            + "-iterations 10 "
            + "-repetitions 1000000 "
            + "-vms 500 "
            + "-timeout 10 "
            + "-measurementStrategy PARALLEL "
//            + "-useGC false "
            + "-version " + endversion + " "
            + "-executionfile $PEASS_REPOS/dependencies-final/" + ResultsFolders.TRACE_SELECTION_PREFIX + name + ".json "
            + "-folder ../projekte/" + name + "/ "
            + " &> measurement_" + endversion.substring(0, 6) + "_" + testcaseName
            + ".txt");*/
      goal.println("./peasstomcat measureSingle "
              + endversion + " "
              + testcaseName);
   }

}
