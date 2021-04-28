package de.dagere.peass.utils;

import java.io.PrintStream;

import de.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterSlurmRCA extends RunCommandWriter {

   public static final String EXECUTE_RCA = "executeRCA.sh";
   
   boolean inited = false;
   private final String script;

   public RunCommandWriterSlurmRCA(final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      this(goal, experimentId, dependencies, RunCommandWriterSlurm.EXECUTE_RCA);
   }

   public RunCommandWriterSlurmRCA(final PrintStream goal, final String experimentId, final SelectedTests dependencies, final String script) {
      super(goal, experimentId, dependencies);
      slurmOutputFolder = "/nfs/user/do820mize/rcalogs/" + dependencies.getName();
      this.script = script;
   }

   public RunCommandWriterSlurmRCA(final PrintStream goal, final String experimentId, final String name, final String url) {
      super(goal, experimentId, name, url);
      slurmOutputFolder = "/nfs/user/do820mize/rcalogs/" + name;
      script = RunCommandWriterSlurm.EXECUTE_RCA;
   }

   public void init() {
      goal.println("timestamp=$(date +%s)");
      goal.println("mkdir -p " + slurmOutputFolder);
   }

   private final String slurmOutputFolder;

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName, int iterations, int repetitions, int vms) {
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
                  + "ITERATIONS=" + iterations + ","
                  + "REPETITIONS=" + repetitions + ","
                  + "VMS=" + vms + ","
                  + "EXPERIMENT_ID=" + experimentId + ","
                  + "TEST=" + testcaseName
                  + " " + script);
   }
}
