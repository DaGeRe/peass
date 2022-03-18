package de.dagere.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ParseException;

import de.dagere.peass.ci.logHandling.LogRedirector;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.VersionIterator;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class ContinuousDependencyReader {

   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);

   private final TestSelectionConfig dependencyConfig;
   private final ExecutionConfig executionConfig;
   private final KiekerConfig kiekerConfig;
   private final PeassFolders folders;
   private final ResultsFolders resultsFolders;
   private final EnvironmentVariables env;

   public ContinuousDependencyReader(final TestSelectionConfig dependencyConfig, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final PeassFolders folders,
         final ResultsFolders resultsFolders, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.executionConfig = executionConfig;
      this.kiekerConfig = new KiekerConfig(kiekerConfig);
      this.kiekerConfig.setUseKieker(true);
      this.kiekerConfig.setRecord(AllowedKiekerRecord.OPERATIONEXECUTION);
      this.kiekerConfig.setUseAggregation(false);
      this.folders = folders;
      this.resultsFolders = resultsFolders;
      this.env = env;
   }

   public RTSResult getTests(final VersionIterator iterator, final String url, final String version, final MeasurementConfig measurementConfig) {
      final StaticTestSelection dependencies = getDependencies(iterator, url);

      RTSResult result;
      final Set<TestCase> tests;
      if (dependencies.getVersions().size() > 0) {
         VersionStaticSelection versionInfo = dependencies.getVersions().get(version);
         LOG.debug("Versioninfo for version {}, running was: {}", version, versionInfo != null ? versionInfo.isRunning() : "null");
         if (dependencyConfig.isGenerateTraces()) {
            tests = selectResults(version);
            result = new RTSResult(tests, versionInfo.isRunning());
         } else {
            tests = versionInfo.getTests().getTests();
            result = new RTSResult(tests, versionInfo.isRunning());
         }

         // final Set<TestCase> tests = selectIncludedTests(dependencies);
         NonIncludedTestRemover.removeNotIncluded(tests, measurementConfig.getExecutionConfig());
      } else {
         tests = new HashSet<>();
         result = new RTSResult(tests, true);
         LOG.info("No test executed - version did not contain changed tests.");
      }
      return result;
   }

   private Set<TestCase> selectResults(final String version) {
      try {
         final Set<TestCase> tests;
         if (dependencyConfig.isGenerateCoverageSelection()) {
            LOG.info("Using coverage-based test selection");
            ExecutionData executionData = Constants.OBJECTMAPPER.readValue(resultsFolders.getCoverageSelectionFile(), ExecutionData.class);
            tests = fetchTestset(version, executionData);
         } else {
            LOG.info("Using dynamic test selection results");
            ExecutionData executionData = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);
            tests = fetchTestset(version, executionData);
         }
         return tests;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Fetches the test set from the current version; it is required to allow null, in case a compile error occured
    * @param version
    * @param executionData
    * @return
    */
   private Set<TestCase> fetchTestset(final String version, final ExecutionData executionData) {
      final Set<TestCase> tests;
      TestSet versionTestSet = executionData.getVersions().get(version);
      tests = versionTestSet != null ? versionTestSet.getTests() : new HashSet<TestCase>();
      return tests;
   }

   StaticTestSelection getDependencies(final VersionIterator iterator, final String url) {
      try {
         StaticTestSelection dependencies;

         final VersionKeeper noChanges = new VersionKeeper(new File(resultsFolders.getStaticTestSelectionFile().getParentFile(), "nonChanges_" + folders.getProjectName() + ".json"));

         if (!resultsFolders.getStaticTestSelectionFile().exists()) {
            LOG.debug("Fully loading dependencies");
            dependencies = fullyLoadDependencies(url, iterator, noChanges);
         } else {
            LOG.debug("Partially loading dependencies");
            dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);
            VersionComparator.setDependencies(dependencies);
            
            if (iterator != null) {
               executePartialRTS(dependencies, iterator);
            }
         }
         VersionComparator.setDependencies(dependencies);

         return dependencies;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private void executePartialRTS(final StaticTestSelection dependencies, final VersionIterator newIterator) throws FileNotFoundException {
      if (executionConfig.isRedirectSubprocessOutputToFile()) {
         File logFile = resultsFolders.getDependencyLogFile(newIterator.getTag(), newIterator.getPredecessor());
         LOG.info("Executing regression test selection update - Log goes to {}", logFile.getAbsolutePath());
         try (LogRedirector director = new LogRedirector(logFile)) {
            doPartialRCS(dependencies, newIterator);
         }
      } else {
         doPartialRCS(dependencies, newIterator);
      }

   }

   private void doPartialRCS(final StaticTestSelection dependencies, final VersionIterator newIterator) {
      DependencyReader reader = new DependencyReader(dependencyConfig, folders, resultsFolders, dependencies.getUrl(), newIterator,
            new VersionKeeper(new File(resultsFolders.getStaticTestSelectionFile().getParentFile(), "nochanges.json")), executionConfig, kiekerConfig, env);
      newIterator.goTo0thCommit();

      reader.readCompletedVersions(dependencies);

      try {
         ExecutionData executions = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);
         reader.setExecutionData(executions);

         if (resultsFolders.getCoverageSelectionFile().exists()) {
            ExecutionData coverageExecutions = Constants.OBJECTMAPPER.readValue(resultsFolders.getCoverageSelectionFile(), ExecutionData.class);
            reader.setCoverageExecutions(coverageExecutions);

            if (resultsFolders.getCoverageInfoFile().exists()) {
               CoverageSelectionInfo coverageInfo = Constants.OBJECTMAPPER.readValue(resultsFolders.getCoverageInfoFile(), CoverageSelectionInfo.class);
               reader.setCoverageInfo(coverageInfo);
            }
         }

         reader.readDependencies();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   private StaticTestSelection fullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws Exception {
      if (executionConfig.isRedirectSubprocessOutputToFile()) {
         File logFile = resultsFolders.getDependencyLogFile(iterator.getTag(), iterator.getPredecessor());
         LOG.info("Executing regression test selection - Log goes to {}", logFile.getAbsolutePath());

         try (LogRedirector director = new LogRedirector(logFile)) {
            return doFullyLoadDependencies(url, iterator, nonChanges);
         }
      } else {
         return doFullyLoadDependencies(url, iterator, nonChanges);
      }
   }

   private StaticTestSelection doFullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws IOException, InterruptedException, XmlPullParserException, JsonParseException, JsonMappingException, ParseException, ViewNotFoundException {
      final DependencyReader reader = new DependencyReader(dependencyConfig, folders, resultsFolders, url, iterator, nonChanges, executionConfig, kiekerConfig, env);
      iterator.goToPreviousCommit();
      if (!reader.readInitialVersion()) {
         LOG.error("Analyzing first version did not yield results");
      } else {
         reader.readDependencies();
      }
      StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);
      return dependencies;
   }
}
