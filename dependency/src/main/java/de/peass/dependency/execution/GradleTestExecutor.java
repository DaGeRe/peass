package de.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.GradleParseUtil.FindDependencyVisitor;
import de.peass.testtransformation.JUnitTestTransformer;

public class GradleTestExecutor extends TestExecutor {

   private static final Logger LOG = LogManager.getLogger(GradleTestExecutor.class);

   private boolean isAndroid;

   public GradleTestExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer, final long timeout) {
      super(folders, timeout, testTransformer);
   }

   @Override
   public void executeAllKoPeMeTests(final File logFile) {
      prepareKoPeMeExecution(logFile);
      try {
         final int testcount = getTestCount();
         final Process process = buildProcess(folders.getProjectFolder(), logFile);
         LOG.info("Starting Process, tests: {}", testcount);
         final long timeout = 1l + testcount * this.timeout;
         execute("all", timeout, process);
      } catch (InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }

   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      LOG.debug("Starting Test Transformation");
      transformTests();
      if (testTransformer.isUseKieker()) {
         generateAOPXML();
      }

      try {
         final Path tempFiles = Files.createTempDirectory("kiekerTemp");
         lastTmpFile = tempFiles.toFile();
         boolean anyIsAndroid = false;
         for (final File module : getModules()) {
            final File gradleFile = new File(module, "build.gradle");
            FindDependencyVisitor visitor;
            if (testTransformer.isUseKieker()) {
               visitor = GradleParseUtil.addDependency(gradleFile, "de.dagere.kopeme:kopeme-junit:" + MavenPomUtil.KOPEME_VERSION,
                     MavenTestExecutor.TEMP_DIR + ":" + lastTmpFile.getAbsolutePath());
            } else {
               visitor = GradleParseUtil.addDependency(gradleFile, "de.dagere.kopeme:kopeme-junit:" + MavenPomUtil.KOPEME_VERSION, null);
            }
            if (visitor.isAndroid()) {
               anyIsAndroid = true;
            }
         }
         isAndroid = anyIsAndroid;
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }

   }

   protected Process buildProcess(final File folder, final File logFile, final String... commandLineAddition) throws IOException, XmlPullParserException, InterruptedException {
      final String[] originals;
      if (isAndroid) {
         originals = new String[] { new File(folders.getProjectFolder(), "gradlew").getAbsolutePath(),
               "--init-script", new File(folders.getGradleHome(), "init.gradle").getAbsolutePath(),
               "--no-daemon",
               "cleanTest", "testRelease" };
      } else {
         originals = new String[] { new File(folders.getProjectFolder(), "gradlew").getAbsolutePath(),
               "--init-script", new File(folders.getGradleHome(), "init.gradle").getAbsolutePath(),
               "--no-daemon",
               "cleanTest", "test" };
      }

      final String[] vars = new String[commandLineAddition.length + originals.length];
      for (int i = 0; i < originals.length; i++) {
         vars[i] = originals[i];
      }
      for (int i = 0; i < commandLineAddition.length; i++) {
         vars[originals.length + i] = commandLineAddition[i];
      }

      return buildFolderProcess(folder, logFile, vars);
   }

   private void generateAOPXML() {
      try {
         for (final File module : getModules()) {
            final File metainf = new File(module, "src/main/resources/META-INF");
            metainf.mkdirs();
            final File goalFile = new File(metainf, "aop.xml");
            AOPXMLHelper.writeAOPXMLToFile(existingClasses, goalFile);
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   /**
    * Since older gradle versions do not re-execute tests, the build-folder need to be cleared before test re-execution
    */
   public void cleanLastTest(final File module) {
      final File testFolder = new File(module, "build" + File.separator + "test-results" + File.separator + "release");
      if (testFolder.exists()) {
         LOG.debug("Cleaning: {}", testFolder);
         try {
            FileUtils.deleteDirectory(testFolder);
         } catch (final IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.debug("Cleaning not necessary: {}", testFolder);
      }
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      final File module = new File(folders.getProjectFolder(), test.getModule());
      cleanLastTest(module);
      final ChangedEntity testClazzEntity = new ChangedEntity(test.getClazz(), test.getModule());
      runMethod(logFolder, testClazzEntity, module, test.getMethod(), timeout);
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   protected void runTest(final File module, final File logFile, final String testname, final long timeout) {
      try {
         final Process process = buildProcess(module, logFile, "--tests", testname);
         execute(testname, timeout, process);
      } catch (final InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   @Override
   public boolean isVersionRunning(final String version) {
      final File wrapper = new File(folders.getProjectFolder(), "gradlew");
      final File potentialBuildfile = new File(folders.getProjectFolder(), "build.gradle");
      boolean isRunning = false;
      buildfileExists = wrapper.exists() && potentialBuildfile.exists();
      if (buildfileExists) {
         try {
            boolean isAndroid = false;
            for (final File module : getModules()) {
               final File buildfile = new File(module, "build.gradle");
               final FindDependencyVisitor visitor = GradleParseUtil.setAndroidTools(buildfile);
               if (visitor.isAndroid()) {
                  isAndroid = true;
                  if (!visitor.hasVersion()) {
                     return false;
                  }
               }
            }
            this.isAndroid = isAndroid;
         } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
         }
         final String[] vars = new String[] { "./gradlew", "--no-daemon", "assemble" };
         isRunning = testRunningSuccess(version, vars);
      }
      return isRunning;
   }

   @Override
   public List<File> getModules() throws IOException, XmlPullParserException {
      return GradleParseUtil.getModules(folders.getProjectFolder());
   }

   @Override
   public boolean isAndroid() {
      return isAndroid;
   }
}
