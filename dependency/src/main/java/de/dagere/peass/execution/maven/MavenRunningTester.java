package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.execution.CommandConcatenator;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.MavenPomUtil;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.execution.processutils.ProcessSuccessTester;

public class MavenRunningTester {
   
   private static final Logger LOG = LogManager.getLogger(MavenRunningTester.class);
   
   private final PeASSFolders folders;
   private final EnvironmentVariables env;
   private final MeasurementConfiguration measurementConfig;
   private final ProjectModules modules;
   private boolean buildfileExists;
   
   public MavenRunningTester(final PeASSFolders folders, final EnvironmentVariables env, final MeasurementConfiguration measurementConfig, final ProjectModules modules) {
      this.folders = folders;
      this.env = env;
      this.measurementConfig = measurementConfig;
      this.modules = modules;
   }

   public boolean isVersionRunning(final String version) {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      final File potentialPom = pomFile;
      final File testFolder = new File(folders.getProjectFolder(), "src/test");
      boolean isRunning = false;
      buildfileExists = potentialPom.exists();
      if (potentialPom.exists()) {
         try {
            final boolean multimodule = MavenPomUtil.isMultiModuleProject(potentialPom);
            if (multimodule || testFolder.exists()) {
               new MavenUpdater(folders, modules, measurementConfig).updateJava();
               String goal = "test-compile";
               if (folders.getProjectName().equals("jetty.project")) {
                  goal = "package";
               }
               MavenPomUtil.cleanType(pomFile);
               String[] basicParameters = new String[] { env.fetchMavenCall(),
                     "clean", goal,
                     "-DskipTests",
                     "-Dmaven.test.skip.exec"};
               String[] withMavendefaults = CommandConcatenator.concatenateCommandArrays(basicParameters, CommandConcatenator.mavenCheckDeactivation);
               if (measurementConfig.getExecutionConfig().getPl() != null) {
                  String[] projectListArray = new String[] { "-pl", measurementConfig.getExecutionConfig().getPl(), "-am" };
                  String[] withPl = CommandConcatenator.concatenateCommandArrays(withMavendefaults, projectListArray);
                  isRunning = new ProcessSuccessTester(folders, measurementConfig, env).testRunningSuccess(version, withPl);
               } else {
                  isRunning = new ProcessSuccessTester(folders, measurementConfig, env).testRunningSuccess(version, withMavendefaults);
               }
            } else {
               LOG.error("Expected src/test to exist");
               return false;
            }
         } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("No pom.xml in {}", version);
      }
      return isRunning;
   }
   
   public boolean isBuildfileExists() {
      return buildfileExists;
   }
   
}
