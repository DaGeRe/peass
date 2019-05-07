package de.peass.utils;

import java.io.PrintStream;

import de.peass.dependency.persistence.Dependencies;

public class RunCommandWriter {
   private final PrintStream goal;
   private final boolean writeSlurm;
   private final String experimentId;
   // final Dependencies dependencies;
   private String name;
   private String url;

   public RunCommandWriter(PrintStream goal, boolean writeSlurm, String experimentId, final Dependencies dependencies) {
      super();
      this.goal = goal;
      this.writeSlurm = writeSlurm;
      this.experimentId = experimentId;
      name = dependencies.getName();
      url = dependencies.getUrl();
      if (writeSlurm) {
         System.out.println("timestamp=$(date +%s)");
      }
   }

   public RunCommandWriter(PrintStream goal, boolean writeSlurm, String experimentId, String name, String url) {
      super();
      this.goal = goal;
      this.writeSlurm = writeSlurm;
      this.experimentId = experimentId;
      this.name = name;
      this.url = url;
      if (writeSlurm) {
         System.out.println("timestamp=$(date +%s)");
      }
   }

   public void createFullVersionCommand(int versionIndex, final String endversion) {
      if (writeSlurm) {
         goal.println(
               "sbatch --nice=1000000 --time=10-0 "
                     + "--output=/nfs/user/do820mize/processlogs/process_" + versionIndex + "_$timestamp.out "
                     + "--workdir=/nfs/user/do820mize "
                     + "--export=PROJECT=" + url + ",HOME=/newnfs/user/do820mize,START="
                     + endversion + ",END=" + endversion + ",INDEX=" + versionIndex + " executeTests.sh");
      } else {
         throw new RuntimeException("Not implemented yet.");
      }
   }

   public void createSingleMethodCommand(final int versionIndex, final String endversion, final String testcaseName) {
      if (writeSlurm) {
         final String simpleTestName = testcaseName.substring(testcaseName.lastIndexOf('.') + 1);
         goal.println(
               "sbatch --partition=galaxy-low-prio --nice=1000000 --time=10-0 "
                     + "--output=/nfs/user/do820mize/processlogs/" + versionIndex + "_" + simpleTestName + "_$timestamp.out "
                     + "--workdir=/nfs/user/do820mize "
                     + "--export=PROJECT=" + url + ",HOME=/nfs/user/do820mize,"
                     + "START=" + endversion + ","
                     + "END=" + endversion + ","
                     + "INDEX=" + versionIndex + ","
                     + "EXPERIMENT_ID=" + experimentId + ","
                     + "TEST=" + testcaseName + " executeTests.sh");
      } else {
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
               + ".txt\n");
      }
   }

}
