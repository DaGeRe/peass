package de.peass;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.dependencyprocessors.PairProcessor;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.testtransformation.TimeBasedTestTransformer;
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
@Command(description = "Measures the defined tests and versions until the number of VMs is reached", name = "measure")
public class DependencyTestStarter extends PairProcessor {
   
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
   
   @Option(names = { "-useGC", "--useGC" }, description = "Do execute GC before each iteration (default false)")
   public boolean useGC = false;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;
   
   @Option(names = { "-earlyStop", "--earlyStop" }, description = "Whether to stop early (i.e. execute VMs until type 1 and type 2 error are met)")
   protected boolean earlyStop = false;
   
   @Option(names = { "-type1error", "--type1error" }, description = "Type 1 error of agnostic-t-test, i.e. probability of considering measurements equal when they are unequal (requires earlyStop)")
   public double type1error = 0.05;

   @Option(names = { "-type2error", "--type2error" }, description = "Type 2 error of agnostic-t-test, i.e. probability of considering measurements unequal when they are equal (requires earlyStop)")
   protected double type2error = 0.01;
   
   JUnitTestTransformer getTestTransformer(final MeasurementConfiguration measurementConfig) {
      final JUnitTestTransformer testtransformer = new JUnitTestTransformer(folders.getProjectFolder(), measurementConfig);
      return testtransformer;
   }

   private static final Logger LOG = LogManager.getLogger(DependencyTestStarter.class);

   protected DependencyTester tester;
   private final List<String> versions = new LinkedList<>();
   private int startindex, endindex;
   private TestCase test;

   @Override
   public Void call() throws Exception {
      super.call();
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(timeout, vms, type1error, type2error);
      measurementConfiguration.setEarlyStop(earlyStop);
      measurementConfiguration.setUseKieker(useKieker);
      measurementConfiguration.setIterations(iterations);
      measurementConfiguration.setWarmup(warmup);
      measurementConfiguration.setRepetitions(repetitions);
      measurementConfiguration.setUseGC(useGC);
      if (duration != 0) {
         TimeBasedTestTransformer testTransformer = new TimeBasedTestTransformer(folders.getProjectFolder());
         ((TimeBasedTestTransformer) testTransformer).setDuration(duration);
         if (repetitions != 1) {
            testTransformer.getConfig().setRepetitions(repetitions);
         }
         testTransformer.getConfig().setIterations(0);
         testTransformer.getConfig().setWarmup(0);
         testTransformer.getConfig().setUseKieker(useKieker);
         tester = new DependencyTester(folders, testTransformer);
      } else {
         final JUnitTestTransformer testgenerator = getTestTransformer(measurementConfiguration);
         
         tester = new DependencyTester(folders, testgenerator);
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
      
      processCommandline();
      return null;
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
               for (final String executionVersion : executionData.getVersions().keySet()) {
                  for (final String dependencyVersion : dependencies.getVersions().keySet()) {
                     if (dependencyVersion.equals(executionVersion)) {
                        potentialStart = dependencyVersion;
                        break;
                     }
                  }
                  if (executionVersion.equals(startversion)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executefile, next version in dependencyfile: {}", potentialStart);
            currentStartindex = versions.indexOf(potentialStart);
            if (currentStartindex == -1) {
               throw new RuntimeException("Did not find " + startversion + " in given PRONTO-files!");
            }
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
               for (final String executionVersion : executionData.getVersions().keySet()) {
                  boolean next = false;
                  for (final String dependencyVersion : dependencies.getVersions().keySet()) {
                     if (next) {
                        potentialStart = dependencyVersion;
                        break;
                     }
                     if (dependencyVersion.equals(executionVersion)) {
                        next = true;
                     }
                  }
                  if (executionVersion.equals(endversion)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executionfile, next version in dependencyfile: {}", potentialStart);
            currentEndindex = versions.indexOf(potentialStart);
         }
      }
      return currentEndindex;
   }

   @Override
   protected void processVersion(final String version, final Version versioninfo) {
      LOG.debug("Configuration: VMs: {} Warmup: {} Iterations: {} Repetitions: {}", vms, warmup, iterations, repetitions);
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
                     tester.setVersions(version, versionOld);
                     tester.evaluate(testcase);
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
      final DependencyTestStarter command = new DependencyTestStarter();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

}
