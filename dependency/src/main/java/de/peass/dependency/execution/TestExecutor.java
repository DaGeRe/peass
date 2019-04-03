package de.peass.dependency.execution;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.kopeme.BuildtoolProjectNameReader;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.peass.dependency.ClazzFinder;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.StreamGobbler;
import de.peass.vcs.GitUtils;

/**
 * Base functionality for executing tests with KoPeMe. The executor automates changing the buildfile, changing the tests both with and without Kieker.
 * 
 * @author reichelt
 *
 */
public abstract class TestExecutor {

   private static final Logger LOG = LogManager.getLogger(TestExecutor.class);

   public static final String GENERATED_TEST_NAME = "GeneratedTest";

   protected final PeASSFolders folders;
   protected File lastTmpFile;
   protected int jdk_version = 8;
   /** This is the timeout in minutes */
   protected final long timeout;
   protected final JUnitTestTransformer testTransformer;
   protected List<String> existingClasses;

   protected boolean buildfileExists = false;

   public TestExecutor(final PeASSFolders folders, final long timeout, final JUnitTestTransformer testTransformer) {
      this.folders = folders;
      this.timeout = timeout;
      this.testTransformer = testTransformer;
   }

   public void setJDKVersion(final int jdk_version) {
      this.jdk_version = jdk_version;
   }

   public int getJDKVersion() {
      return jdk_version;
   }

   public abstract void prepareKoPeMeExecution(File logFile) throws IOException, InterruptedException;

   public abstract void executeAllKoPeMeTests(final File logFile);

   public abstract void executeTest(final TestCase tests, final File logFolder, long timeout);

   protected int getTestCount() throws IOException, XmlPullParserException {
      final List<TestCase> testcases = getTestCases();
      return testcases.size();
   }

   protected List<TestCase> getTestCases() throws IOException, XmlPullParserException {
      final List<TestCase> testcases = new LinkedList<>();
      for (final File module : getModules()) {
         for (final String test : ClazzFinder.getTestClazzes(new File(module, "src"))) {
            final String moduleName = ModuleClassMapping.getModuleName(folders.getProjectFolder(), module);
            final ChangedEntity entity = new ChangedEntity(test, moduleName);
            final List<String> testMethods = testTransformer.getTests(module, entity);
            for (final String method : testMethods) {
               final TestCase tc = new TestCase(test, method, moduleName);
               testcases.add(tc);
            }
         }
      }
      return testcases;
   }

   protected Process buildFolderProcess(final File currentFolder, final File logFile, final String[] vars) throws IOException {
      LOG.debug("Command: {}", vars);

      final ProcessBuilder pb = new ProcessBuilder(vars);
      LOG.debug("KOPEME_HOME={}", folders.getTempMeasurementFolder().getAbsolutePath());
      pb.environment().put("KOPEME_HOME", folders.getTempMeasurementFolder().getAbsolutePath());
      if (this instanceof GradleTestExecutor) {
         pb.environment().put("GRADLE_HOME", folders.getGradleHome().getAbsolutePath());
      }
      LOG.debug("LD_LIBRARY_PATH: {}", System.getenv().get("LD_LIBRARY_PATH"));
      for (final Map.Entry<String, String> env : System.getenv().entrySet()) {
         pb.environment().put(env.getKey(), env.getValue());
      }

      pb.directory(currentFolder);
      if (logFile != null) {
         pb.redirectOutput(Redirect.appendTo(logFile));
         pb.redirectError(Redirect.appendTo(logFile));
      }

      final Process process = pb.start();
      final int pid = Integer.parseInt(new File("/proc/self").getCanonicalFile().getName());
      LOG.debug("Process started: {} Used PIDs: {}", pid, getProcessCount());
      return process;
   }

   protected abstract void runTest(File module, final File logFile, final String testname, final long timeout);

   void runMethod(final File logFolder, final ChangedEntity clazz, final File module, final String method, final long timeout) {
      testTransformer.shortenClazz(module, clazz, method);

      // final ChangedEntity generatedClazz = new ChangedEntity(clazz.getPackage() + "." + GENERATED_TEST_NAME, "");
      // final File generatedClazzFile = testTransformer.generateClazz(module, generatedClazz, clazz, method);
      //
      final File logFile = new File(logFolder, "log_" + clazz.getJavaClazzName() + File.separator + method + ".txt");
      if (!logFile.getParentFile().exists()) {
         logFile.getParentFile().mkdir();
      }
      runTest(module, logFile, clazz.getJavaClazzName() + "#" + method, timeout);
      
      testTransformer.resetShortenedFile();
      
//      GitUtils.reset(folders.getProjectFolder(), calleeClazzFile);
      
      //
      // renameGeneratedData(module, generatedClazz, clazz, method);
      // generatedClazzFile.delete();
   }

   void renameGeneratedData(final File module, final ChangedEntity generatedClazz, final ChangedEntity clazz, final String method) {
      final BuildtoolProjectNameReader reader = new BuildtoolProjectNameReader();
      final File assumedResultFolder;
      final File dest;
      if (reader.foundPomXml(module, 1)) {
         // final ProjectInfo info = reader.getProjectInfo();
         final File projectFolder = new File(folders.getTempMeasurementFolder(), reader.getGroupId() + File.separator + reader.getArtifactId());
         assumedResultFolder = new File(projectFolder, generatedClazz.getJavaClazzName());
         dest = new File(projectFolder, clazz.getClazz());
      } else {
         assumedResultFolder = new File(folders.getTempMeasurementFolder(), clazz.getModule() + File.separator + generatedClazz.getJavaClazzName());
         dest = new File(folders.getTempMeasurementFolder(), clazz.getModule() + File.separator + clazz.getClazz());
      }

      if (assumedResultFolder.exists()) {
         renameFromFolder(clazz, method, assumedResultFolder, dest);
      } else {
         LOG.error("Problem: {} does not exist", assumedResultFolder.getAbsolutePath());
      }
   }

   void renameFromFolder(final ChangedEntity clazz, final String method, final File assumedResultFolder, final File dest) {
      LOG.debug("Renaming: {}", assumedResultFolder);

      dest.getParentFile().mkdirs();
      LOG.debug("Dest: {}", dest.getAbsolutePath());
      if (!dest.exists() && assumedResultFolder.renameTo(dest)) {
         final File resultFile = new File(dest, method + ".xml");
         LOG.debug(resultFile.exists());
         if (resultFile.exists()) {
            renameFileContents(clazz, resultFile);
         } else {
            LOG.error("Resultfile did not exist: {}", resultFile.getAbsolutePath());
         }
      } else {
         final File resultFile = new File(assumedResultFolder, method + ".xml");
         if (resultFile.exists()) {
            final File destResultFile = new File(dest, method + ".xml");
            LOG.debug("Dest: {}", destResultFile.getAbsolutePath());
            if (!destResultFile.exists()) {
               if (resultFile.renameTo(destResultFile)) {
                  renameFileContents(clazz, destResultFile);
                  for (final File otherFile : assumedResultFolder.listFiles()) {
                     if (otherFile.getName().matches("[0-9]+")) {
                        final File destKiekerFile = new File(dest, otherFile.getName());
                        otherFile.renameTo(destKiekerFile);
                     }
                  }
               } else {
                  throw new RuntimeException("Fatal error: Renaiming failed!");
               }
            } else {
               LOG.error("Resultfile {} does not exist", resultFile.getAbsolutePath());
            }
         } else {
            LOG.error("Old resultfile was not cleaned: {}", dest.getAbsolutePath());
         }
      }
   }

   void renameFileContents(final ChangedEntity clazz, final File resultFile) {
      try {
         final Kopemedata data = new XMLDataLoader(resultFile).getFullData();
         if (!data.getTestcases().getClazz().equals(clazz.getPackage() + "." + GENERATED_TEST_NAME)) {
            throw new RuntimeException("Fatal error:  Wrong class executed: " + data.getTestcases().getClazz());
         }
         data.getTestcases().setClazz(clazz.getClazz());
         final List<TestcaseType> testcases = data.getTestcases().getTestcase();
         if (testcases.size() != 1) {
            throw new RuntimeException("Fatal error: More than one testcase was executed!");
         }
         XMLDataStorer.storeData(resultFile, data);
      } catch (final JAXBException e) {
         e.printStackTrace();
      }
   }

   public synchronized static int getProcessCount() {
      int count = -1;
      try {
         final Process process = new ProcessBuilder(new String[] { "bash", "-c", "ps -e -T | wc -l" }).start();
         final String result = StreamGobbler.getFullProcess(process, false).replaceAll("\n", "").replace("\r", "");
         count = Integer.parseInt(result.trim());
      } catch (IOException | NumberFormatException e) {

         e.printStackTrace();
      }
      return count;
   }

   public void transformTests() {
      try {
         final List<File> modules = getModules();
         testTransformer.determineVersions(modules);
         testTransformer.transformTests();
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private final List<String> aborted = new LinkedList<>();

   protected void execute(final String testname, final long timeout, final Process process) throws InterruptedException, IOException {
      if (timeout == -1) {
         LOG.info("Executing without timeout!");
         process.waitFor();
      } else {
         LOG.debug("Executing: {} Timeout: {}", testname, timeout);
         process.waitFor(timeout, TimeUnit.MINUTES);
         if (process.isAlive()) {
            LOG.debug("Killing: {}", testname);
            process.destroy();
            aborted.add(testname);
            FileUtils.writeStringToFile(new File(folders.getFullMeasurementFolder(), "aborted.txt"), aborted.toString(), Charset.defaultCharset());
         }
      }

   }

   protected boolean testRunningSuccess(final String version, final String[] vars) {
      boolean isRunning = false;
      try {
         final ProcessBuilder pb = new ProcessBuilder(vars);
         pb.directory(folders.getProjectFolder());
         if (this instanceof GradleTestExecutor) {
            pb.environment().put("GRADLE_HOME", folders.getGradleHome().getAbsolutePath());
         }
         System.out.println(folders.getProjectFolder());
         final File versionFolder = getVersionFolder(version);
         final File logFile = new File(versionFolder, "testRunning.log");
         pb.redirectOutput(Redirect.appendTo(logFile));
         pb.redirectError(Redirect.appendTo(logFile));

         LOG.debug("Waiting for {} minutes", timeout);
         final Process process = pb.start();
         process.waitFor(timeout, TimeUnit.MINUTES);
         if (process.isAlive()) {
            LOG.debug("Destroying process");
            process.destroyForcibly().waitFor();
         }
         final int returncode = process.exitValue();
         if (returncode != 0) {
            isRunning = false;
         } else {
            isRunning = true;
         }
      } catch (final IOException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
      return isRunning;
   }

   /**
    * Tells whether currently checkout out version has a buildfile - is only set correctly after isVersionRunning has been called for current version.
    * 
    * @return
    */
   public boolean doesBuildfileExist() {
      return buildfileExists;
   }

   public abstract boolean isVersionRunning(String version);

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

   public File getVersionFolder(final String version) {
      final File versionFolder = new File(folders.getLogFolder(), version);
      if (!versionFolder.exists()) {
         versionFolder.mkdir();
      }
      return versionFolder;
   }

   public abstract List<File> getModules() throws IOException, XmlPullParserException;

   public List<String> getExistingClasses() {
      return existingClasses;
   }

   public void loadClasses() {
      existingClasses = new LinkedList<>();
      try {
         for (final File module : getModules()) {
            existingClasses.addAll(ClazzFinder.getClasses(module));
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }

   public boolean isAndroid() {
      return false;
   }

   public long getTimeout() {
      return timeout;
   }
}
