package de.dagere.peass.execution.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.typeFinding.TypeFileFinder;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.execution.kieker.KiekerEnvironmentPreparer;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Base functionality for executing performance tests, both instrumented and not instrumented. The executor automates changing the buildfile, changing the tests both with and
 * without Kieker.
 *
 * @author reichelt
 */
public abstract class TestExecutor {

   private static final Logger LOG = LogManager.getLogger(TestExecutor.class);

   public static final String GENERATED_TEST_NAME = "GeneratedTest";

   protected final PeassFolders folders;
   protected File lastTmpFile;
   protected int jdk_version = 8;
   protected final TestTransformer testTransformer;
   protected List<String> existingClasses;
   protected Set<String> includedMethodPattern;
   protected boolean isAndroid;

   protected final EnvironmentVariables env;

   public TestExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      this.folders = folders;
      this.testTransformer = testTransformer;
      this.env = env;
   }

   public void setJDKVersion(final int jdk_version) {
      this.jdk_version = jdk_version;
   }

   public int getJDKVersion() {
      return jdk_version;
   }

   public abstract void prepareKoPeMeExecution(File logFile);

   public abstract void executeTest(final TestMethodCall test, final File logFolder, long timeout);

   public abstract void executeTest(final String javaAgent, final TestMethodCall test, final File logFolder, long timeout);

   /**
    * Deletes files which are bigger than sizeInMb Mb, since they pollute the disc space and will not be analyzable
    *
    * @param folderToClean
    */
   public void cleanAboveSize(final File folderToClean, final String ending) {
      for (final File file : FileUtils.listFiles(folderToClean, new WildcardFileFilter("*." + ending), TrueFileFilter.INSTANCE)) {
         final long size = file.length() / (1024 * 1024);
         LOG.debug("File: {} Size: {} MB", file, size);
         if (size > testTransformer.getConfig().getMaxLogSizeInMb()) {
            LOG.debug("Deleting file.");
            file.delete();
         }
      }
   }

   protected File getCleanLogFile(final File logFolder, final TestMethodCall test) {
      File clazzLogFolder = getClazzLogFolder(logFolder, test);
      final File logFile = new File(clazzLogFolder, "clean" + File.separator + test.getMethodWithParams() + ".txt");
      if (!logFile.getParentFile().exists()) {
         logFile.getParentFile().mkdir();
      }
      return logFile;
   }

   protected File getMethodLogFile(final File logFolder, final TestMethodCall test) {
      File clazzLogFolder = getClazzLogFolder(logFolder, test);
      final File logFile = new File(clazzLogFolder, test.getMethodWithParams() + ".txt");
      return logFile;
   }

   private File getClazzLogFolder(final File logFolder, final TestCase test) {
      File clazzLogFolder;
      if (test.getModule() != null && !"".equals(test.getModule())) {
         File moduleFolder = new File(logFolder, test.getModule());
         if (!moduleFolder.exists()) {
            moduleFolder.mkdir();
         }
         clazzLogFolder = new File(moduleFolder, "log_" + test.getClazz());
      } else {
         clazzLogFolder = new File(logFolder, "log_" + test.getClazz());
      }
      if (!clazzLogFolder.exists()) {
         clazzLogFolder.mkdir();
      }

      return clazzLogFolder;
   }

   protected void prepareKiekerSource() {
      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         final KiekerEnvironmentPreparer kiekerEnvironmentPreparer = new KiekerEnvironmentPreparer(includedMethodPattern, existingClasses, folders, testTransformer,
               getModules().getModules());
         kiekerEnvironmentPreparer.prepareKieker();
      }
   }

   private final List<String> aborted = new LinkedList<>();

   protected void execute(final String testname, final long timeoutInSeconds, final Process process) {
      if (timeoutInSeconds == -1) {
         LOG.warn("Executing without timeout!");
         try {
            process.waitFor();
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      } else if (timeoutInSeconds > 0) {
         try {
            LOG.debug("Executing: {} Timeout: {}", testname, timeoutInSeconds);
            process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
            if (process.isAlive()) {
               LOG.debug("Killing: {}", testname);
               process.destroyForcibly().waitFor();
               aborted.add(testname);
               FileUtils.writeStringToFile(new File(folders.getFullMeasurementFolder(), "aborted.txt"), aborted.toString(), Charset.defaultCharset());
            }
         } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
         }
      } else {
         throw new RuntimeException("Illegal timeout: " + timeoutInSeconds);
      }
   }

   /**
    * Tells whether currently checkout out version has a buildfile
    *
    * @return
    */
   public abstract boolean doesBuildfileExist();

   public abstract boolean isCommitRunning(String version);

   /**
    * Deletes temporary files, in order to not get memory problems
    */
   public void deleteTemporaryFiles() {
      try {
         if (lastTmpFile != null && lastTmpFile.exists()) {
            final File[] tmpKiekerStuff = lastTmpFile.listFiles((FilenameFilter) new WildcardFileFilter("kieker*"));
            for (final File kiekerFolder : tmpKiekerStuff) {
               LOG.debug("Deleting: {}", kiekerFolder.getAbsolutePath());
               FileUtils.deleteDirectory(kiekerFolder);
            }
            FileUtils.deleteDirectory(lastTmpFile);
         }

      } catch (final IOException | IllegalArgumentException e) {
         LOG.info("Problems deleting last temp file..");
         e.printStackTrace();
      }
   }

   /**
    * Returns the {@link ProjectModules} of the current project; if null is returned, then the project is not parseable in the current commit.
    *
    * @return {@link ProjectModules} of the current project or null.
    */
   public abstract ProjectModules getModules();

   public List<String> getExistingClasses() {
      return existingClasses;
   }

   protected abstract void clean(final File logFile) throws IOException, InterruptedException;

   public void fetchClasses(final ModuleClassMapping mapping) {
      existingClasses = new LinkedList<>();
      existingClasses.addAll(mapping.getAllClasses());

   }

   public void loadClasses() {
      existingClasses = new LinkedList<>();
      for (final File module : getModules().getModules()) {
         ExecutionConfig executionConfig = testTransformer.getConfig().getExecutionConfig();
         TypeFileFinder finder = new TypeFileFinder(executionConfig);
         final List<String> currentClasses = finder.getTypes(module);
         existingClasses.addAll(currentClasses);
      }
   }

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }

   public PeassFolders getFolders() {
      return folders;
   }

   public boolean isAndroid() {
      return false;
   }

   public void setIncludedMethods(final Set<String> includedMethodPattern) {
      this.includedMethodPattern = includedMethodPattern;
   }

   public TestTransformer getTestTransformer() {
      return testTransformer;
   }

   public EnvironmentVariables getEnv() {
      return env;
   }
}
