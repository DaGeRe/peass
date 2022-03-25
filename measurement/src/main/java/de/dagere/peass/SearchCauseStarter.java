package de.dagere.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
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
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
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

   public static void main(final String[] args) throws JAXBException, IOException {
      final SearchCauseStarter command = new SearchCauseStarter();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   public SearchCauseStarter() throws JAXBException, IOException {
      super();
   }

   @Override
   public Void call() throws Exception {
      if (testName == null) {
         throw new RuntimeException("Test needs to be defined!");
      }

      initVersionProcessor();

      if (version == null) {
         version = executionData.getVersions().keySet().iterator().next();
         LOG.info("Version was not defined, using " + version);
      }

      final TestCase test = new TestCase(testName);
      final VersionStaticSelection versionInfo = staticTestSelection.getVersions().get(version);
      boolean found = versionInfo.getTests().getTests().contains(test);
      if (!found) {
         LOG.error("Test " + test + " is not contained in regression test selection result, therefore it is unlikely to have a performance change!");
      }

      final String predecessor = versionInfo.getPredecessor();

      LOG.debug("Timeout in minutes: {}", executionMixin.getTimeout());
      final MeasurementConfig measurementConfiguration = getConfiguration(predecessor);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, causeSearchConfigMixin);

      if (!kiekerConfigMixin.isNotUseAggregation() && measurementConfiguration.getKiekerConfig().getRecord() == AllowedKiekerRecord.OPERATIONEXECUTION) {
         throw new RuntimeException("Aggregation and OperationExecutionRecord can not be combined!");
      }

      if (kiekerConfigMixin.isNotUseAggregation() && measurementConfiguration.getKiekerConfig().getRecord() == AllowedKiekerRecord.DURATION) {
         throw new RuntimeException("Non-aggregation and duration record cannot be combined, since duration records make it impossible to detect place in call tree");
      }

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(folders.getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, alternateFolders, new EnvironmentVariables(measurementConfiguration.getExecutionConfig().getProperties()));

      final CauseSearcher tester = getCauseSeacher(measurementConfiguration, causeSearcherConfig, alternateFolders, reader);
      tester.search();

      return null;
   }

   private MeasurementConfig getConfiguration(final String predecessor) {
      final MeasurementConfig measurementConfiguration = new MeasurementConfig(measurementConfigMixin, executionMixin, statisticConfigMixin, kiekerConfigMixin);
      measurementConfiguration.setUseKieker(true);
      measurementConfiguration.getExecutionConfig().setVersion(version);
      measurementConfiguration.getExecutionConfig().setVersionOld(predecessor);

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

   public static CauseSearcher getCauseSeacher(final MeasurementConfig measurementConfiguration,
         final CauseSearcherConfig causeSearcherConfig, final CauseSearchFolders alternateFolders, final BothTreeReader reader) throws IOException, InterruptedException {
      if (measurementConfiguration.getKiekerConfig().isOnlyOneCallRecording()) {
         throw new RuntimeException("isOnlyOneCallRecording is not allowed to be set to true for RCA!");
      }

      EnvironmentVariables env = reader.getEnv();
      final CauseSearcher tester;
      final CauseTester measurer = new CauseTester(alternateFolders, measurementConfiguration, causeSearcherConfig, env);
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
                  return new SourceChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor(), config.getPropertyFolder(), measurementConfiguration);
               }
            };
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creatorSource, env);
            break;
         case UNTIL_STRUCTURE_CHANGE:
            TreeAnalyzerCreator creator = new TreeAnalyzerCreator() {

               @Override
               public TreeAnalyzer getAnalyzer(final BothTreeReader reader, final CauseSearcherConfig config) {
                  return new StructureChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
               }
            };
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creator, env);
            break;
         default:
            throw new RuntimeException("Strategy " + causeSearcherConfig.getRcaStrategy() + " not expected");
         }
      } else {
         LOG.info("Defaulting to StructureCauseSearcher");
         TreeAnalyzerCreator creator = new TreeAnalyzerCreator() {

            @Override
            public TreeAnalyzer getAnalyzer(final BothTreeReader reader, final CauseSearcherConfig config) {
               return new StructureChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
            }
         };
         tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creator, env);
      }
      return tester;
   }

}
