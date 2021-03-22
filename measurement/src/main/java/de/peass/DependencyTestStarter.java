package de.peass;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.MeasurementConfiguration;
import de.peass.config.StatisticsConfigurationMixin;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.MeasurementConfigurationMixin;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.dependencyprocessors.PairProcessor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
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

   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;
   
   @Mixin
   protected StatisticsConfigurationMixin statisticConfigMixin;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;

   private static final Logger LOG = LogManager.getLogger(DependencyTestStarter.class);

   protected DependencyTester tester;
   private final List<String> versions = new LinkedList<>();
   private int startindex, endindex;
   private TestCase test;

   @Override
   public Void call() throws Exception {
      super.call();
      final MeasurementConfiguration measurementConfiguration = createConfig();
      createTester(measurementConfiguration);

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

   private void createTester(final MeasurementConfiguration measurementConfiguration) throws IOException {
      if (measurementConfigMixin.getDuration() != 0) {
         throw new RuntimeException("Time-based running currently not supported; eventually fix commented-out code to get it running again");
      } else {
         tester = new DependencyTester(folders, new MeasurementConfiguration(measurementConfigMixin, executionMixin, statisticConfigMixin), new EnvironmentVariables());
      }
   }

   private MeasurementConfiguration createConfig() {
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(measurementConfigMixin, executionMixin, statisticConfigMixin);
      return measurementConfiguration;
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
      LOG.debug("Configuration: VMs: {} Warmup: {} Iterations: {} Repetitions: {}", measurementConfigMixin.getVms(),
            measurementConfigMixin.getWarmup(), measurementConfigMixin.getIterations(), measurementConfigMixin.getRepetitions());
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
      } catch (IOException | InterruptedException | JAXBException | XmlPullParserException e) {
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
      System.exit(commandLine.execute(args));
   }

}
