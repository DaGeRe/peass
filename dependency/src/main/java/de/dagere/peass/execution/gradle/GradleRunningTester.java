package de.dagere.peass.execution.gradle;

import java.io.File;
import java.util.Arrays;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.maven.BuildfileRunningTester;
import de.dagere.peass.execution.processutils.ProcessSuccessTester;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;

public class GradleRunningTester implements BuildfileRunningTester {

   private final PeassFolders folders;
   private final EnvironmentVariables env;
   private final MeasurementConfig measurementConfig;
   private final ProjectModules modules;

   private boolean isAndroid = false;

   public GradleRunningTester(final PeassFolders folders, final MeasurementConfig measurementConfig, final EnvironmentVariables env, final ProjectModules modules) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
      this.modules = modules;
   }

   public boolean isCommitRunning(final String commit, GradleTestExecutor executor) {
      boolean isRunning = false;
      if (executor.doesBuildfileExist()) {
         boolean isAndroid = false;
         for (final File module : modules.getModules()) {
            final File buildfile = GradleParseHelper.findGradleFile(module);
            final GradleBuildfileVisitor visitor = GradleParseUtil.setAndroidTools(buildfile, measurementConfig.getExecutionConfig());
            if (visitor.isAndroid()) {
               isAndroid = true;
               if (!visitor.hasVersion()) {
                  return false;
               }
            }
         }
         this.isAndroid = isAndroid;

         executor.replaceAllBuildfiles(modules);

         final String cleanGoal = measurementConfig.getExecutionConfig().getCleanGoal() != null ? measurementConfig.getExecutionConfig().getCleanGoal() : "clean";
         
         final String[] basicVars = new String[] { EnvironmentVariables.fetchGradleCall(), "--no-daemon" };
         final String[] vars;
         if (!isAndroid) {
            if (measurementConfig.getExecutionConfig().getExecutableCheckGoals().isEmpty()) {
               
               String[] temp = CommandConcatenator.concatenateCommandArrays(basicVars, new String[] { cleanGoal });
               vars = CommandConcatenator.concatenateCommandArrays(temp, measurementConfig.getExecutionConfig().getExecutableCheckGoals().toArray(new String[0]));
            } else {
               vars = CommandConcatenator.concatenateCommandArrays(basicVars, new String[] { cleanGoal, "testClasses", "assemble" });
            }
         } else {
            vars = CommandConcatenator.concatenateCommandArrays(basicVars, new String[] { "assemble" });
         }

         ProcessSuccessTester processSuccessTester = new ProcessSuccessTester(folders, measurementConfig, env);
         isRunning = processSuccessTester.testRunningSuccess(commit, vars);

         File cleanLogFile = folders.getDependencyLogSuccessRunFile(commit);
         GradleDaemonFileDeleter.deleteDaemonFile(cleanLogFile);
      }
      return isRunning;
   }

   public boolean isAndroid() {
      return isAndroid;
   }

   @Override
   public boolean isCommitRunning(String commit, TestExecutor executor) {
      if (!(executor instanceof GradleTestExecutor)) {
         throw new RuntimeException("Can only be called with GradleTestExecutor, but was " + executor.getClass());
      } else {
         return isCommitRunning(commit, (GradleTestExecutor) executor);
      }
   }
}
