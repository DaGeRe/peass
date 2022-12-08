package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.processutils.ProcessSuccessTester;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;

public class MavenRunningTester {

   private static final Logger LOG = LogManager.getLogger(MavenRunningTester.class);

   private final PeassFolders folders;
   private final EnvironmentVariables env;
   private final MeasurementConfig measurementConfig;
   private final ProjectModules modules;
   private boolean buildfileExists;

   public MavenRunningTester(final PeassFolders folders, final MeasurementConfig measurementConfig, final EnvironmentVariables env, final ProjectModules modules) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
      this.modules = modules;
   }

   public boolean isCommitRunning(final String commit) {
      File potentialPom = new File(folders.getProjectFolder(), "pom.xml");
      final File testFolder = new File(folders.getProjectFolder(), "src/test");
      boolean isRunning = false;
      if (potentialPom.exists()) {
         try {
            final boolean multimodule = MavenPomUtil.isMultiModuleProject(potentialPom);
            if (multimodule || testFolder.exists()) {
               new MavenUpdater(folders, modules, measurementConfig).updateJava();
               String goal = getGoal();
               MavenPomUtil.cleanType(potentialPom);
               String mvnCall = env.fetchMavenCall(folders.getProjectFolder());
               String[] basicParameters = new String[] { mvnCall,
                     "--batch-mode",
                     "clean", goal,
                     "-DskipTests",
                     "-Dmaven.test.skip.exec" };
               String[] withMavendefaults = CommandConcatenator.concatenateCommandArrays(basicParameters, CommandConcatenator.mavenCheckDeactivation);
               if (measurementConfig.getExecutionConfig().getPl() != null) {
                  String[] projectListArray = new String[] { "-pl", measurementConfig.getExecutionConfig().getPl(), "-am" };
                  String[] withPl = CommandConcatenator.concatenateCommandArrays(withMavendefaults, projectListArray);
                  isRunning = new ProcessSuccessTester(folders, measurementConfig, env).testRunningSuccess(commit, withPl);
               } else {
                  isRunning = new ProcessSuccessTester(folders, measurementConfig, env).testRunningSuccess(commit, withMavendefaults);
               }
            } else {
               LOG.error("Expected src/test to exist");
               return false;
            }
         } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("No pom.xml in {}", commit);
      }
      return isRunning;
   }

   private String getGoal() {
      String goal = "test-compile";
      if (folders.getProjectName().equals("jetty.project")) {
         goal = "package";
      }
      return goal;
   }
}
