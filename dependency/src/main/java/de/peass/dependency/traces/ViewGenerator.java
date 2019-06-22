package de.peass.dependency.traces;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.DependencyReadingStarter;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.TestResultManager;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.PairProcessor;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.utils.Constants;
import de.peass.utils.OptionConstants;
import de.peass.utils.StreamGobbler;
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
   // private final TestResultManager resultsManager;

   public ViewGenerator(final File projectFolder, final Dependencies dependencies, final File executefile, final File viewFolder, final int threads, final int timeout) {
      super(projectFolder, dependencies, timeout);
      this.viewFolder = viewFolder;
      this.executeFile = executefile;
      this.threads = threads;
      processInitialVersion(dependencies.getInitialversion());
      changedTraceMethods.setAndroid(dependencies.isAndroid());
      init();
   }

   public ViewGenerator() throws ParseException, JAXBException, JsonParseException, JsonMappingException, IOException {

   }

   public void init() {
      final String url = GitUtils.getURL(folders.getProjectFolder());
      changedTraceMethods.setUrl(url);
   }

   public void processVersion(final String version, final Version versioninfo, final ExecutorService threads) {
      LOG.info("View-Generation for Version {}", version);
      final Set<TestCase> testcases = versioninfo.getTests().getTests();

      final boolean beforeEndVersion = endversion == null || version.equals(endversion) || VersionComparator.isBefore(version, endversion);
      LOG.debug("Before End Version {}: {}", endversion, beforeEndVersion);

      final TestSet tests = new TestSet();
      for (final TestCase testcase : testcases) {
         if (!VersionComparator.isBefore(version, startversion) && beforeEndVersion) {
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
            testset, changedTraceMethods, timeout);
   }

   public File getExecuteFile() {
      return executeFile;
   }

   public static void main(String[] args) throws JsonParseException, JsonMappingException, ParseException, JAXBException, IOException {
      CommandLine commandLine = new CommandLine(new ViewGenerator());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      super.call();
      // final File resultFolder = DependencyReadingStarter.getResultFolder();
      final String projectName = folders.getProjectFolder().getName();
      init();

      if (out == null) {
         out = DependencyReadingStarter.getResultFolder();
      }

      executeFile = new File(out, "execute-" + projectName + ".json");
      viewFolder = new File(out, "views_" + projectName);
      if (!viewFolder.exists()) {
         viewFolder.mkdir();
      }

      return null;
   }

}
