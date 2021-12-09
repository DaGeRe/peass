package de.dagere.peass.measurement.utils;

import java.io.PrintStream;

import de.dagere.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterSlurm extends RunCommandWriter {

   public static final String EXECUTE_MEASUREMENT = "executeTests.sh";
   public static final String EXECUTE_RCA = "executeRCA.sh";
   
   boolean inited = false;
   private final String script;

   public RunCommandWriterSlurm(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      this(goal, experimentId, dependencies, EXECUTE_MEASUREMENT);
   }

   public RunCommandWriterSlurm(final PrintStream goal, final String experimentId, final SelectedTests dependencies, final String script) {
      super(goal, experimentId, dependencies);
      slurmOutputFolder = "/nfs/user/do820mize/processlogs/" + dependencies.getName();
      this.script = script;
   }

   public RunCommandWriterSlurm(final PrintStream goal, final String experimentId, final String name, final String url) {
      super(goal, experimentId, name, url);
      slurmOutputFolder = "/nfs/user/do820mize/processlogs/" + name;
      script = EXECUTE_MEASUREMENT;
   }

   public void init() {
      goal.println("timestamp=$(date +%s)");
      goal.println("mkdir -p " + slurmOutputFolder);
   }

   private final String slurmOutputFolder;

   @Override
   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      if (!inited) {
         init();
         inited = true;
      }
      final String simpleTestName = testcaseName.substring(testcaseName.lastIndexOf('.') + 1);
      goal.println(
            "sbatch --partition=galaxy-low-prio --nice=" + nice + " --time=10-0 "
                  + "--output=" + slurmOutputFolder + "/" + versionIndex + "_" + simpleTestName + "_$timestamp.out "
                  + "--export=PROJECT=" + url + ",HOME=/nfs/user/do820mize,"
                  + "START=" + endversion + ","
                  + "END=" + endversion + ","
                  + "INDEX=" + versionIndex + ","
                  + "EXPERIMENT_ID=" + experimentId + ","
                  + "TEST=" + testcaseName
                  + " " + script);
   }
}
