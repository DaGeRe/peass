package de.peass;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.dependencyprocessors.PairProcessor;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.OptionConstants;
import de.peass.utils.TestLoadUtil;
import groovy.util.Eval;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Runs the dependency test by running the test, where something could have changed, pairwise for every new version. This makes it faster to get potential change candidates, but it
 * takes longer for a whole project.
 * 
 * @author reichelt
 *
 */
@Command(description = "Measures the defined tests and versions exactly until the number of VMs is reached", name = "measureExact")
public class DependencyTestPairStarter extends PairProcessor {
   
   @Option(names = { "-vms", "--vms" }, description = "Number of VMs to start")
   int vms = 100;
   
   @Option(names = { "-duration", "--duration" }, description = "Which duration to use - if duration is specified, warmup and iterations are ignored")
   int duration = 0;
   
   @Option(names = { "-warmup", "--warmup" }, description = "Number of warmup iterations")
   int warmup = 10;
   
   @Option(names = { "-iterations", "--iterations" }, description = "Number of iterations")
   int iterations = 1000;
   
   @Option(names = { "-repetitions", "--repetitions" }, description = "Last version that should be analysed")
   int repetitions = 100;
   
   @Option(names = { "-useKieker", "--useKieker", "-usekieker", "--usekieker"}, description = "Whether Kieker should be used")
   boolean useKieker = false;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;

   JUnitTestTransformer getTestTransformer() {
//      final int repetitions = Integer.parseInt(line.getOptionValue(OptionConstants.REPETITIONS.getName(), "100"));
//      final boolean useKieker = Boolean.parseBoolean(line.getOptionValue(OptionConstants.USEKIEKER.getName(), "false"));
//      final int warmup = Integer.parseInt(line.getOptionValue(OptionConstants.WARMUP.getName(), "10"));
//      final int iterations = Integer.parseInt(line.getOptionValue(OptionConstants.ITERATIONS.getName(), "1000"));
      final JUnitTestTransformer testgenerator = new JUnitTestTransformer(folders.getProjectFolder());
      testgenerator.setDatacollectorlist(DataCollectorList.ONLYTIME);
      testgenerator.setIterations(iterations);
      testgenerator.setLogFullData(true);
      testgenerator.setWarmupExecutions(warmup);
      testgenerator.setUseKieker(useKieker);
      testgenerator.setRepetitions(repetitions);
      if (timeout != 0) {
         testgenerator.setSumTime(timeout);
      }
      return testgenerator;
   }

   private static final Logger LOG = LogManager.getLogger(DependencyTestPairStarter.class);

   protected DependencyTester tester;
   private final List<String> versions = new LinkedList<>();
   private int startindex, endindex;
   private TestCase test;

   @Override
   public Void call() throws Exception {
      super.call();
      if (duration != 0) {
         tester = new DependencyTester(folders, duration, vms, true, repetitions, useKieker);
      } else {
         final JUnitTestTransformer testgenerator = getTestTransformer();
         tester = new DependencyTester(folders, testgenerator, new MeasurementConfiguration(vms));
      }

      if (testName != null) {
         test = new TestCase(testName);
         LOG.info("Test: {}", test);
      } else {
         test = null;
      }

      versions.add(dependencies.getInitialversion().getVersion());

      dependencies.getVersions().keySet().forEach(version -> versions.add(version));

      startindex = getStartVersionIndex();
      endindex = getEndVersion();
      return null;
   }
   
   public DependencyTestPairStarter() {
   }

   /**
    * Calculates the index of the start version
    * 
    * @return index of the start version
    */
   private int getStartVersionIndex() {
      int currentStartindex = startversion != null ? versions.indexOf(startversion) : 0;
      // Only bugfix if dependencyfile and executefile do not fully match
      if (executionData != null) {
         if (startversion != null && currentStartindex == -1) {
            String potentialStart = "";
            if (executionData.getVersions().containsKey(startversion)) {
               for (final String sicVersion : executionData.getVersions().keySet()) {
                  for (final String ticVersion : dependencies.getVersions().keySet()) {
                     if (ticVersion.equals(sicVersion)) {
                        potentialStart = ticVersion;
                        break;
                     }
                  }
                  if (sicVersion.equals(startversion)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executefile, next version in dependencyfile: {}", potentialStart);
            currentStartindex = versions.indexOf(potentialStart);
         }
      }
      return currentStartindex;
   }

   /**
    * Calculates the index of the end version.
    * 
    * @return index of the end version
    */
   private int getEndVersion() {
      int currentEndindex = endversion != null ? versions.indexOf(endversion) : versions.size();
      // Only bugfix if dependencyfile and executefile do not fully match
      if (executionData != null) {
         if (endversion != null && currentEndindex == -1) {
            String potentialStart = "";
            if (executionData.getVersions().containsKey(endversion)) {
               for (final String sicVersion : executionData.getVersions().keySet()) {
                  boolean next = false;
                  for (final String ticVersion : dependencies.getVersions().keySet()) {
                     if (next) {
                        potentialStart = ticVersion;
                        break;
                     }
                     if (ticVersion.equals(sicVersion)) {
                        next = true;
                     }
                  }
                  if (sicVersion.equals(endversion)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executefile, next version in dependencyfile: {}", potentialStart);
            currentEndindex = versions.indexOf(potentialStart);
         }
      }
      return currentEndindex;
   }

   @Override
   protected void processVersion(final String version, final Version versioninfo) {
      try {
         final int currentIndex = versions.indexOf(version);
         final boolean executeThisVersion = currentIndex >= startindex && currentIndex <= endindex;

         LOG.trace("Processing Version {} Executing Tests: {}", version, executeThisVersion);

         final Set<TestCase> testcases = versioninfo.getTests().getTests();
         final String versionOld = versioninfo.getPredecessor();

         for (final TestCase testcase : testcases) {
            if (executeThisVersion) {
               if (lastTestcaseCalls.containsKey(testcase)) {
                  boolean executeThisTest = true;
                  if (test != null) {
                     executeThisTest = checkTestName(testcase, executeThisTest);
                  }

                  if (executeThisTest) {
                     executeThisTest = checkExecutionData(version, testcase, executeThisTest);
                  }
                  if (executeThisTest) {
                     tester.evaluate(version, versionOld, testcase);
                  }
               }
            }
            lastTestcaseCalls.put(testcase, version);
         }
      } catch (IOException | InterruptedException | JAXBException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected void postEvaluate() {
      tester.postEvaluate();
   }

   public boolean checkExecutionData(final String version, final TestCase testcase, boolean executeThisTest) {
      if (executionData != null) {
         final TestSet calls = executionData.getVersions().get(version);
         boolean hasChanges = false;
         if (calls != null) {
            for (final Map.Entry<ChangedEntity, Set<String>> clazzCalls : calls.entrySet()) {
               final String changedClazz = clazzCalls.getKey().getJavaClazzName();
               if (changedClazz.equals(testcase.getClazz()) && clazzCalls.getValue().contains(testcase.getMethod())) {
                  hasChanges = true;
               }
            }
         }
         if (!hasChanges) {
            LOG.debug("Skipping " + testcase + " because of execution-JSON in " + version);
            executeThisTest = false;
         }
      }
      return executeThisTest;
   }

   public boolean checkTestName(final TestCase testcase, boolean executeThisTest) {
      LOG.debug("Checking " + test + " " + testcase);
      if (!test.equals(testcase)) {
         executeThisTest = false;
         LOG.debug("Skipping: " + testcase);
      } else {
         LOG.debug("Success!");
      }
      return executeThisTest;
   }

   public static void main(final String[] args) throws JAXBException, IOException {
      DependencyTestPairStarter command = new DependencyTestPairStarter();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
      command.processCommandline();
      // final DependencyTestPairStarter starter = new DependencyTestPairStarter(args);
      // starter.processCommandline();
   }

}
