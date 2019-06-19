package de.peass.utils;

import java.io.PrintStream;

import de.peass.dependency.persistence.SelectedTests;

public class RunCommandWriterSlurm extends RunCommandWriter {

   public RunCommandWriterSlurm(PrintStream goal, String experimentId, SelectedTests dependencies) {
      super(goal, experimentId, dependencies);
      slurmOutputFolder = "/nfs/user/do820mize/processlogs/" + dependencies.getName();

      init(goal);
   }

   public RunCommandWriterSlurm(PrintStream goal, String experimentId, String name, String url) {
      super(goal, experimentId, name, url);
      slurmOutputFolder = "/nfs/user/do820mize/processlogs/" + name;
      
      init(goal);
   }
   
   public void init(PrintStream goal) {
      goal.println("timestamp=$(date +%s)");
      goal.println("mkdir -p " + slurmOutputFolder);
   }

   private final String slurmOutputFolder;

   public void createFullVersionCommand(int versionIndex, final String endversion) {
      goal.println(
            "sbatch --nice=" + nice + " --time=10-0 "
                  + "--output=" + slurmOutputFolder + "/process_" + versionIndex + "_$timestamp.out "
                  + "--workdir=/nfs/user/do820mize "
                  + "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,START="
                  + endversion + ",END=" + endversion + ",INDEX=" + versionIndex + " executeTests.sh");
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      final String simpleTestName = testcaseName.substring(testcaseName.lastIndexOf('.') + 1);
      goal.println(
            "sbatch --partition=galaxy-low-prio --nice=" + nice + " --time=10-0 "
                  + "--output=" + slurmOutputFolder + "/" + versionIndex + "_" + simpleTestName + "_$timestamp.out "
                  + "--workdir=/nfs/user/do820mize "
                  + "--export=PROJECT=" + url + ",HOME=/nfs/user/do820mize,"
                  + "START=" + endversion + ","
                  + "END=" + endversion + ","
                  + "INDEX=" + versionIndex + ","
                  + "EXPERIMENT_ID=" + experimentId + ","
                  + "TEST=" + testcaseName + " executeTests.sh");
   }
}
