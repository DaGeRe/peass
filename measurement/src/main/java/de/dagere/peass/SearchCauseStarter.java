package de.dagere.peass;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.analysis.properties.ChangedMethodManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.measurement.rca.analyzer.SourceChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.analyzer.StructureChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.analyzer.TreeAnalyzer;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.measurement.rca.searcher.CauseSearcherComplete;
import de.dagere.peass.measurement.rca.searcher.ICauseSearcher;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.dagere.peass.measurement.rca.searcher.SamplingCauseSearcher;
import de.dagere.peass.measurement.rca.searcher.TreeAnalyzerCreator;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(description = "Searches for root cause of a performance change, i.e. method causing the performance change", name = "searchcause")
public class SearchCauseStarter extends MeasureStarter {

   private static final Logger LOG = LogManager.getLogger(SearchCauseStarter.class);

   @Mixin
   private CauseSearcherConfigMixin causeSearchConfigMixin;

   @Mixin
   private KiekerConfigMixin kiekerConfigMixin;

   public static void main(final String[] args) {
      final SearchCauseStarter command = new SearchCauseStarter();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   public SearchCauseStarter() {
      super();
   }

   @Override
   public Void call() throws Exception {
      if (testName == null) {
         throw new RuntimeException("Test needs to be defined!");
      }

      initCommitProcessor();

      if (commit == null) {
         commit = executionData.getCommits().keySet().iterator().next();
         LOG.info("Commit was not defined, using {}", commit);
      }

      TestMethodCall test = determineTest();

      final String predecessor = staticTestSelection.getCommits().get(commit).getPredecessor();

      LOG.debug("Timeout in minutes: {}", executionMixin.getTimeout());
      final MeasurementConfig measurementConfiguration = getConfiguration(predecessor);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, causeSearchConfigMixin);

      if (!kiekerConfigMixin.isNotUseAggregation() && measurementConfiguration.getKiekerConfig().getRecord() == AllowedKiekerRecord.OPERATIONEXECUTION) {
         throw new RuntimeException("Aggregation and OperationExecutionRecord can not be combined!");
      }

      if (kiekerConfigMixin.isNotUseAggregation() && measurementConfiguration.getKiekerConfig().getRecord() == AllowedKiekerRecord.DURATION) {
         throw new RuntimeException("Non-aggregation and duration record cannot be combined, since duration records make it impossible to detect place in call tree");
      }
      // Only AGGREGATED_WRITER is currently implemented with Anbox. See `AOPXMLHelper.writeKiekerMonitoringProperties()`
      if (kiekerConfigMixin.isNotUseAggregation() && measurementConfiguration.getExecutionConfig().isUseAnbox()) {
         throw new RuntimeException("Non-aggregation and Anbox cannot be combined");
      }

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(folders.getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, alternateFolders,
            new EnvironmentVariables(measurementConfiguration.getExecutionConfig().getProperties()));

      CommitComparatorInstance comparator = new CommitComparatorInstance(staticTestSelection);
      final ICauseSearcher tester = getCauseSeacher(measurementConfiguration, causeSearcherConfig, alternateFolders, reader, comparator);
      tester.search();

      return null;
   }

   private TestMethodCall determineTest() {
      TestMethodCall test = TestMethodCall.createFromString(testName);
      final CommitStaticSelection commitInfo = staticTestSelection.getCommits().get(commit);
      boolean found = false;
      for (TestMethodCall selectedTest : commitInfo.getTests().getTestMethods()) {
         if (selectedTest.getClazz().equals(test.getClazz()) && selectedTest.getMethodWithParams().equals(test.getMethodWithParams())) {
            found = true;
            test = selectedTest; // required to add module
         }
      }
      if (!found) {
         LOG.error("Test {} is not contained in regression test selection result, therefore it is unlikely to have a performance change!", test);
      }
      return test;
   }

   private MeasurementConfig getConfiguration(final String predecessor) {
      final MeasurementConfig measurementConfiguration = new MeasurementConfig(measurementConfigMixin, executionMixin, statisticConfigMixin, kiekerConfigMixin);
      measurementConfiguration.getKiekerConfig().setUseKieker(true);
      measurementConfiguration.getFixedCommitConfig().setCommit(commit);
      measurementConfiguration.getFixedCommitConfig().setCommitOld(predecessor);

      if (causeSearchConfigMixin.getStrategy().equals(RCAStrategy.COMPLETE)) {
         measurementConfiguration.getKiekerConfig().setEnableAdaptiveMonitoring(false);
      } else {
         boolean useSourceInstrumentation = measurementConfiguration.getKiekerConfig().isUseSourceInstrumentation();
         measurementConfiguration.getKiekerConfig().setEnableAdaptiveMonitoring(!useSourceInstrumentation);
      }
      if (kiekerConfigMixin.isNotUseSourceInstrumentation() && kiekerConfigMixin.isNotUseSelectiveInstrumentation()) {
         measurementConfiguration.getKiekerConfig().setUseSelectiveInstrumentation(false);
      } else {
         measurementConfiguration.getKiekerConfig().setUseSelectiveInstrumentation(true);
      }
      if (kiekerConfigMixin.isNotUseSourceInstrumentation() && measurementConfiguration.getExecutionConfig().getTestTransformer().equals(WorkloadType.JMH.getTestTransformer())) {
         throw new RuntimeException("AspectJ instrumentation and jmh currently not implemented!");
      }

      LOG.info("Use source instrumentation: {}", kiekerConfigMixin.isNotUseSourceInstrumentation());
      return measurementConfiguration;
   }

   public static ICauseSearcher getCauseSeacher(final MeasurementConfig measurementConfiguration,
         final CauseSearcherConfig causeSearcherConfig, final CauseSearchFolders alternateFolders, final BothTreeReader reader, CommitComparatorInstance comparator)
         throws IOException, InterruptedException {
      if (measurementConfiguration.getKiekerConfig().isOnlyOneCallRecording()) {
         throw new RuntimeException("isOnlyOneCallRecording is not allowed to be set to true for RCA!");
      }
      if (measurementConfiguration.isDirectlyMeasureKieker()) {
         throw new RuntimeException("directlyMeasureKieker is not allowed to be set to true for RCA!");
      }

      EnvironmentVariables env = reader.getEnv();
      final ICauseSearcher tester;
      final CauseTester measurer = new CauseTester(alternateFolders, measurementConfiguration, causeSearcherConfig, env, comparator);
      if (causeSearcherConfig.getRcaStrategy() != null) {
         switch (causeSearcherConfig.getRcaStrategy()) {
         case COMPLETE:
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, env);
            break;
         case LEVELWISE:
            tester = new LevelCauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, env);
            break;
         case UNTIL_SOURCE_CHANGE:
            if (causeSearcherConfig.getPropertyFolder() == null) {
               throw new RuntimeException("Property folder with correct source code needs to be defined if strategy is UNTIL_SOURCE_CHANGE!");
            }
            TreeAnalyzerCreator creatorSource = new TreeAnalyzerCreator() {

               @Override
               public TreeAnalyzer getAnalyzer(final BothTreeReader reader, final CauseSearcherConfig config) {
                  File propertyFolder = config.getPropertyFolder();
                  File methodSourceFolder = new File(propertyFolder, "methods");
                  ChangedMethodManager manager = new ChangedMethodManager(methodSourceFolder);
                  return new SourceChangeTreeAnalyzer(reader.getRootCurrent(), reader.getRootPredecessor(), manager, measurementConfiguration);
               }
            };
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creatorSource, env);
            break;
         case UNTIL_STRUCTURE_CHANGE:
            TreeAnalyzerCreator creator = new TreeAnalyzerCreator() {

               @Override
               public TreeAnalyzer getAnalyzer(final BothTreeReader reader, final CauseSearcherConfig config) {
                  return new StructureChangeTreeAnalyzer(reader.getRootCurrent(), reader.getRootPredecessor());
               }
            };
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creator, env);
            break;
         case SAMPLING:
            measurementConfiguration.setUseKieker(false);
            tester = new SamplingCauseSearcher(causeSearcherConfig.getTestCase(), measurementConfiguration, alternateFolders, env, causeSearcherConfig, reader);
            break;
         default:
            throw new RuntimeException("Strategy " + causeSearcherConfig.getRcaStrategy() + " not expected");
         }
      } else {
         LOG.info("Defaulting to StructureCauseSearcher");
         TreeAnalyzerCreator creator = new TreeAnalyzerCreator() {

            @Override
            public TreeAnalyzer getAnalyzer(final BothTreeReader reader, final CauseSearcherConfig config) {
               return new StructureChangeTreeAnalyzer(reader.getRootCurrent(), reader.getRootPredecessor());
            }
         };
         tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creator, env);
      }
      return tester;
   }

}
