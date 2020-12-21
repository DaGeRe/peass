package de.peass.dependency.traces;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.PairProcessor;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitUtils;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ViewGenerator extends PairProcessor {

   private static final Logger LOG = LogManager.getLogger(ViewGenerator.class);

   @Option(names = { "-out", "--out" }, description = "Path for saving the executionfile")
   File out;

   private File viewFolder;
   private File executeFile;
   private ExecutionData changedTraceMethods = new ExecutionData();
   private int timeoutInMinutes;
   // private final TestResultManager resultsManager;

   public ViewGenerator(final File projectFolder, final Dependencies dependencies, final File executefile, final File viewFolder, final int threads, final int timeoutInMinutes) {
      super(projectFolder, dependencies);
      this.viewFolder = viewFolder;
      this.executeFile = executefile;
      this.threads = threads;
      processInitialVersion(dependencies.getInitialversion());
      changedTraceMethods.setAndroid(dependencies.isAndroid());
      this.timeoutInMinutes = timeoutInMinutes;
      init();
   }

   public ViewGenerator() throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {
      timeoutInMinutes = 5;
   }

   public void init() {
      final String url = GitUtils.getURL(folders.getProjectFolder());
      changedTraceMethods.setUrl(url);
   }

   public void processVersion(final String version, final Version versioninfo, final ExecutorService threads) {
      LOG.info("View-Generation for Version {}", version);
      final Set<TestCase> testcases = versioninfo.getTests().getTests();

      final boolean beforeEndVersion = endversion == null || version.equals(endversion) || VersionComparator.isBefore(version, endversion);
      final boolean beforeStartVersion = startversion == null || !VersionComparator.isBefore(version, startversion);
      LOG.debug("Before Start Version {}: {}", startversion, beforeStartVersion);
      LOG.debug("Before End Version {}: {}", endversion, beforeEndVersion);

      
      final TestSet tests = new TestSet();
      for (final TestCase testcase : testcases) {
         if (beforeStartVersion && beforeEndVersion) {
            if (lastTestcaseCalls.containsKey(testcase)) {
               tests.addTest(testcase);
            }
         }
         lastTestcaseCalls.put(testcase, version);
      }
      if (!tests.getTestcases().isEmpty()) {
         // int index= VersionComparator.getVersionIndex(versioninfo.getVersion());
         final String predecessor = getRunningPredecessor(version);
         final Runnable currentVersionAnalyser = createGeneratorRunnable(version, predecessor, tests);
         threads.submit(currentVersionAnalyser);
      }
   }

   private String getRunningPredecessor(final String version) {
      String predecessor = VersionComparator.getPreviousVersion(version);
      boolean running = isVersionRunning(predecessor);
      if (running) {
         return version + "~1";
      } else {
         LOG.debug("Previous version {}  of {} not running, searching running predecessor", predecessor, version);
      }
      while (!running && !predecessor.equals(VersionComparator.NO_BEFORE)) {
         predecessor = VersionComparator.getPreviousVersion(predecessor);
         running = isVersionRunning(predecessor);
      }
      return predecessor;
   }

   private boolean isVersionRunning(final String version) {
      boolean running = false;
      for (final Map.Entry<String, Version> previousCandidate : dependencies.getVersions().entrySet()) {
         if (previousCandidate.getKey().equals(version) && previousCandidate.getValue().isRunning()) {
            running = true;
         }
      }
      if (dependencies.getInitialversion().getVersion().equals(version)) {
         return true;
      }
      return running;
   }

   @Override
   public void processVersion(final String version, final Version versioninfo) {
      LOG.info("View-Generation for Version {} Index: {}", version, VersionComparator.getVersionIndex(version));
      final Set<TestCase> testcases = versioninfo.getTests().getTests();

      final boolean beforeEndVersion = endversion == null || version.equals(endversion) || VersionComparator.isBefore(version, endversion);
      LOG.debug("Before End Version {}: {}", endversion, beforeEndVersion);

      final TestSet tests = new TestSet();
      for (final TestCase testcase : testcases) {
         if ((startversion == null || !VersionComparator.isBefore(version, startversion)) && beforeEndVersion) {
            if (lastTestcaseCalls.containsKey(testcase)) {
               tests.addTest(testcase);
            }
         }
         lastTestcaseCalls.put(testcase, version);
      }
      LOG.debug("Testcases for {}: {}", version, tests.classCount());
      if (tests.classCount() > 0) {
         final String predecessor = getRunningPredecessor(version);
         final Runnable currentVersionAnalyser = createGeneratorRunnable(version, predecessor, tests);
         currentVersionAnalyser.run();
      } else {
         LOG.debug("No testcase is executed in {}", version);
      }
   }

   private Runnable createGeneratorRunnable(final String version, final String predecessor, final TestSet testset) {
      LOG.info("Starting {}", version);
      return new ViewGeneratorThread(version, predecessor, folders,
            viewFolder, executeFile,
            testset, changedTraceMethods, timeoutInMinutes);
   }

   public File getExecuteFile() {
      return executeFile;
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, ParseException, JAXBException, IOException {
      final CommandLine commandLine = new CommandLine(new ViewGenerator());
      commandLine.execute(args);
   }

   public ExecutionData getChangedTraceMethods() {
      return changedTraceMethods;
   }

   @Override
   public Void call() throws Exception {
      super.call();
      // final File resultFolder = DependencyReadingStarter.getResultFolder();
      final String projectName = folders.getProjectFolder().getName();
      init();

      if (out == null) {
         out = new File("results");
      }

      executeFile = new File(out, "execute-" + projectName + ".json");
      viewFolder = new File(out, "views_" + projectName);
      if (!viewFolder.exists()) {
         viewFolder.mkdir();
      }

      processCommandline();

      return null;
   }

}
