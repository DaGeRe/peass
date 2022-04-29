package de.dagere.peass.dependency.statistics;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Analyzes a dependency file and prints some statistical information about it.
 * 
 * @author reichelt
 *
 */
public class DependencyStatisticAnalyzer implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(DependencyStatisticAnalyzer.class);

   @Option(names = { "-startversion", "--startversion" }, description = "startversion")
   String startversion;

   @Option(names = { "-endversion", "--endversion" }, description = "endversion")
   String endversion;

   @Option(names = { "-dependencyFile", "--dependencyFile" }, description = "Dependencyfile", required = true)
   File dependencyFile;
   
   @Option(names = { "-executionFile", "--executionFile" }, description = "executionFile")
   File executionFile;

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new DependencyStatisticAnalyzer());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
   // final File dependenciesFile = new File(args[0]);
      if (!dependencyFile.exists()) {
         LOG.info("Dependencies-file " + dependencyFile.getAbsolutePath() + " should exist.");
         System.exit(1);
      }

      ExecutionData changedTests;
      if (executionFile != null) {
         changedTests = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
      } else {
         changedTests = null;
      }

      final DependencyStatistics statistics = getChangeStatistics(dependencyFile, changedTests);

      LOG.info("Versions: {} Bei Pruning ausgeführte Tests: {} Trace-Changed Tests: {}", statistics.size, statistics.pruningRunTests, statistics.changedTraceTests);
      LOG.info("Gesamt-Tests: {} Bei Pruning (ggf. mehrmals) genutzte Tests: {} Nur einmal ausgeführte Tests (d.h. keine Veränderung möglich): {}", statistics.overallRunTests,
            statistics.multipleChangedTest.size(), statistics.onceChangedTests.size());
      return null;
   }

   public static DependencyStatistics getChangeStatistics(final File dependenciesFile, final ExecutionData changedTests)
         throws JsonParseException, JsonMappingException, IOException {
      final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependenciesFile, StaticTestSelection.class);
      final Map<String, VersionStaticSelection> versions = dependencies.getVersions();

      final int startTestCound = dependencies.getInitialversion().getInitialDependencies().size();
      final List<TestCase> currentContainedTests = new LinkedList<>();
      for (final TestCase dependency : dependencies.getInitialversion().getInitialDependencies().keySet()) {
         currentContainedTests.add(dependency);
      }

      LOG.trace("StartTest: {}", startTestCound);
      // final List<TestCase> sometimesChangedTest = new LinkedList<>(); // Nicht nur Vorkommen, auch Anzahl relevant
      final DependencyStatistics statistics = new DependencyStatistics();
      // final List<TestCase> onlyOnceChangedTests = new LinkedList<>();
      statistics.onceChangedTests.addAll(currentContainedTests);

      statistics.size = versions.size();
      // final int changedTraceTests = 0;
      // final int pruningRunTests = 0;
      for (final Entry<String, VersionStaticSelection> version : versions.entrySet()) {
         final Set<TestCase> currentIterationTests = new HashSet<>();
         for (final Map.Entry<ChangedEntity, TestSet> dependency : version.getValue().getChangedClazzes().entrySet()) {
            for (final Entry<TestCase, Set<String>> testcase : dependency.getValue().getTestcases().entrySet()) {
               final String testclass = testcase.getKey().getClazz();
               for (final String method : testcase.getValue()) {
                  final TestCase testcase2 = new TestCase(testclass, method);
                  // final String testname = testclass + "." + method;
                  if (currentContainedTests.contains(testcase2)) {
                     currentIterationTests.add(testcase2);
                  } else {
                     currentContainedTests.add(testcase2);
                     statistics.onceChangedTests.add(testcase2);
                     // LOG.info("Neuer Test: " + testname);
                  }
               }
            }
         }
         int currentTraceChangedTests = 0;
         if (changedTests != null) {
            for (final TestCase currentIterationTest : currentContainedTests) {
               if (changedTests.versionContainsTest(version.getKey(), currentIterationTest)) {
                  currentTraceChangedTests++;
               }
            }
         }

         LOG.trace("Version: {} Tests: {} Trace-Changed: {}", version.getKey(), currentIterationTests.size(), currentTraceChangedTests);
         statistics.multipleChangedTest.addAll(currentIterationTests);
         statistics.onceChangedTests.removeAll(currentIterationTests);

         statistics.changedTraceTests += currentTraceChangedTests;

         statistics.pruningRunTests += currentIterationTests.size();

         statistics.overallRunTests += currentContainedTests.size();
      }
      return statistics;
   }
}
