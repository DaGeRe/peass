package de.dagere.peass;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependencyprocessors.PairProcessor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.measurement.dependencyprocessors.DependencyTester;
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
public class MeasureStarter extends PairProcessor {

   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;
   
   @Mixin
   protected StatisticsConfigMixin statisticConfigMixin;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;

   private static final Logger LOG = LogManager.getLogger(MeasureStarter.class);

   protected DependencyTester tester;
   private final List<String> versions = new LinkedList<>();
   private int startindex, endindex;
   private TestCase test;

   @Override
   public Void call() throws Exception {
      super.call();
      final MeasurementConfig measurementConfiguration = createConfig();
      createTester(measurementConfiguration);

      if (testName != null) {
         test = new TestCase(testName);
         LOG.info("Test: {}", test);
      } else {
         test = null;
      }

      versions.add(staticTestSelection.getInitialversion().getVersion());

      staticTestSelection.getVersions().keySet().forEach(version -> versions.add(version));

      startindex = getStartVersionIndex();
      endindex = getEndVersion();

      processCommandline();
      return null;
   }

   private void createTester(final MeasurementConfig measurementConfiguration) throws IOException {
      if (measurementConfigMixin.getDuration() != 0) {
         throw new RuntimeException("Time-based running currently not supported; eventually fix commented-out code to get it running again");
      } else {
         tester = new DependencyTester(folders, measurementConfiguration, new EnvironmentVariables(measurementConfiguration.getExecutionConfig().getProperties()));
      }
   }

   private MeasurementConfig createConfig() {
      final MeasurementConfig measurementConfiguration = new MeasurementConfig(measurementConfigMixin, executionMixin, 
            statisticConfigMixin, new KiekerConfigMixin());
      return measurementConfiguration;
   }

   /**
    * Calculates the index of the start version
    * 
    * @return index of the start version
    */
   private int getStartVersionIndex() {
      int currentStartindex = startversion != null ? versions.indexOf(startversion) : 0;
      // Only bugfix if static selection file and execution file do not fully match
      if (executionData != null) {
         if (startversion != null && currentStartindex == -1) {
            String potentialStart = "";
            if (executionData.getVersions().containsKey(startversion)) {
               for (final String executionVersion : executionData.getVersions().keySet()) {
                  for (final String dependencyVersion : staticTestSelection.getVersions().keySet()) {
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
            LOG.debug("Version only in executefile, next version in static selection file: {}", potentialStart);
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
      // Only bugfix if static selection file and execution file do not fully match
      if (executionData != null) {
         if (endversion != null && currentEndindex == -1) {
            String potentialStart = "";
            if (executionData.getVersions().containsKey(endversion)) {
               for (final String executionVersion : executionData.getVersions().keySet()) {
                  boolean next = false;
                  for (final String dependencyVersion : staticTestSelection.getVersions().keySet()) {
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
   protected void processVersion(final String version, final VersionStaticSelection versioninfo) {
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
            for (final Map.Entry<TestCase, Set<String>> clazzCalls : calls.entrySet()) {
               final String changedClazz = clazzCalls.getKey().getClazz();
               if (changedClazz.equals(testcase.getClazz()) && clazzCalls.getValue().contains(testcase.getMethodWithParams())) {
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
      final MeasureStarter command = new MeasureStarter();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

}
