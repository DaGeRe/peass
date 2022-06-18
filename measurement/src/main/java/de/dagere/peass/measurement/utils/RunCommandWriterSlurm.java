package de.dagere.peass.measurement.utils;

import java.io.PrintStream;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterSlurm extends RunCommandWriter {

   public static final String EXECUTE_MEASUREMENT = "executeTests.sh";
   public static final String EXECUTE_RCA = "executeRCA.sh";
   
   private static final String HOME_FOLDER = "/home/sc.uni-leipzig.de/do820mize";
   
   boolean inited = false;
   private final String script;

   public RunCommandWriterSlurm(MeasurementConfig config, final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      this(config,goal, experimentId, dependencies, EXECUTE_MEASUREMENT);
   }

   public RunCommandWriterSlurm(MeasurementConfig config, final PrintStream goal, final String experimentId, final SelectedTests dependencies, final String script) {
      super(config, goal, experimentId, dependencies);
      slurmOutputFolder =  HOME_FOLDER +"/processlogs/" + dependencies.getName();
      this.script = script;
   }

   public RunCommandWriterSlurm(final PrintStream goal, final String experimentId, final String name, final String url) {
      super(goal, experimentId, name, url);
      slurmOutputFolder = HOME_FOLDER + "/processlogs/" + name;
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
                  + "--export=PROJECT=" + url + ",HOME=" + HOME_FOLDER +","
                  + "START=" + endversion + ","
                  + "END=" + endversion + ","
                  + "INDEX=" + versionIndex + ","
                  + "EXPERIMENT_ID=" + experimentId + ","
                  + "TEST=" + testcaseName
                  + " " + script);
   }
}
