package de.dagere.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Version;
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
public class RootCauseAnalysis extends DependencyTestStarter {

   private static final Logger LOG = LogManager.getLogger(RootCauseAnalysis.class);

   @Mixin
   private CauseSearcherConfigMixin causeSearchConfigMixin;

   @Mixin
   private KiekerConfigMixin kiekerConfigMixin;

   public static void main(final String[] args) throws JAXBException, IOException {
      final RootCauseAnalysis command = new RootCauseAnalysis();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   public RootCauseAnalysis() throws JAXBException, IOException {
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
      final Version versionInfo = dependencies.getVersions().get(version);
      boolean found = versionInfo.getTests().getTests().contains(test);
      if (!found) {
         LOG.error("Test " + test + " is not contained in regression test selection result, therefore it is unlikely to have a performance change!");
      }

      final String predecessor = versionInfo.getPredecessor();

      LOG.debug("Timeout in minutes: {}", executionMixin.getTimeout());
      final MeasurementConfiguration measurementConfiguration = getConfiguration(predecessor);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, causeSearchConfigMixin);

      if (causeSearcherConfig.isUseAggregation() && measurementConfiguration.getKiekerConfig().getRecord() == AllowedKiekerRecord.OPERATIONEXECUTION) {
         throw new RuntimeException("Aggregation and OperationExecutionRecord can not be combined!");
      }

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(folders.getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, alternateFolders, new EnvironmentVariables());

      final CauseSearcher tester = getCauseSeacher(measurementConfiguration, causeSearcherConfig, alternateFolders, reader);
      tester.search();

      return null;
   }

   private MeasurementConfiguration getConfiguration(final String predecessor) {
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(measurementConfigMixin, executionMixin, statisticConfigMixin);
      measurementConfiguration.setUseKieker(true);
      measurementConfiguration.setKiekerAggregationInterval(kiekerConfigMixin.getWriteInterval());
      measurementConfiguration.getExecutionConfig().setVersion(version);
      measurementConfiguration.getExecutionConfig().setVersionOld(predecessor);
      boolean useSourceInstrumentation = !kiekerConfigMixin.isNotUseSourceInstrumentation();
      measurementConfiguration.getKiekerConfig().setUseSourceInstrumentation(useSourceInstrumentation);
      if (causeSearchConfigMixin.getStrategy().equals(RCAStrategy.COMPLETE)) {
         measurementConfiguration.getKiekerConfig().setEnableAdaptiveMonitoring(false);
      } else {
         measurementConfiguration.getKiekerConfig().setEnableAdaptiveMonitoring(!useSourceInstrumentation);
      }
      measurementConfiguration.getKiekerConfig().setAdaptiveInstrumentation(kiekerConfigMixin.isEnableAdaptiveInstrumentation());
      measurementConfiguration.getKiekerConfig().setUseCircularQueue(kiekerConfigMixin.isUseCircularQueue());
      if (kiekerConfigMixin.isNotUseSourceInstrumentation() && kiekerConfigMixin.isNotUseSelectiveInstrumentation()) {
         measurementConfiguration.getKiekerConfig().setUseSelectiveInstrumentation(false);
      } else {
         measurementConfiguration.getKiekerConfig().setUseSelectiveInstrumentation(true);
      }
      if (kiekerConfigMixin.isNotUseSourceInstrumentation() && measurementConfiguration.getExecutionConfig().getTestTransformer().equals(WorkloadType.JMH.getTestTransformer())) {
         throw new RuntimeException("AspectJ instrumentation and jmh currently not implemented!");
      }

      measurementConfiguration.getKiekerConfig().setUseAggregation(kiekerConfigMixin.isUseSampling());
      if (kiekerConfigMixin.isNotUseSourceInstrumentation() && kiekerConfigMixin.isUseExtraction()) {
         throw new RuntimeException("Deactivated source instrumentation and usage of extraction is not possible!");
      }
      measurementConfiguration.getKiekerConfig().setExtractMethod(kiekerConfigMixin.isUseExtraction());
      LOG.info("Use source instrumentation: {}", kiekerConfigMixin.isNotUseSourceInstrumentation());
      return measurementConfiguration;
   }

   public static CauseSearcher getCauseSeacher(final MeasurementConfiguration measurementConfiguration,
         final CauseSearcherConfig causeSearcherConfig, final CauseSearchFolders alternateFolders, final BothTreeReader reader) throws IOException, InterruptedException {
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
