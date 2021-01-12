package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.persistence.Version;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseSearcherConfigMixin;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.analyzer.CompleteTreeAnalyzer;
import de.peass.measurement.rca.analyzer.SourceChangeTreeAnalyzer;
import de.peass.measurement.rca.analyzer.StructureChangeTreeAnalyzer;
import de.peass.measurement.rca.analyzer.TreeAnalyzer;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.searcher.CauseSearcher;
import de.peass.measurement.rca.searcher.CauseSearcherComplete;
import de.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.measurement.rca.searcher.TreeAnalyzerCreator;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysisteetime.model.analysismodel.statistics.PredefinedUnits;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

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
      final String predecessor = versionInfo.getPredecessor();

      LOG.debug("Timeout in minutes: {}", measurementConfigMixin.getTimeout());
      final MeasurementConfiguration measurementConfiguration = getConfiguration(predecessor);
      final JUnitTestTransformer testtransformer = getTestTransformer(measurementConfiguration);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, causeSearchConfigMixin);
      final CauseSearchFolders alternateFolders = new CauseSearchFolders(folders.getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, alternateFolders);

      final CauseSearcher tester = getCauseSeacher(measurementConfiguration, testtransformer, causeSearcherConfig, alternateFolders, reader);
      tester.search();

      return null;
   }

   private MeasurementConfiguration getConfiguration(final String predecessor) {
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(measurementConfigMixin);
      measurementConfiguration.setUseKieker(true);
      measurementConfiguration.setKiekerAggregationInterval(kiekerConfigMixin.getWriteInterval());
      measurementConfiguration.setVersion(version);
      measurementConfiguration.setVersionOld(predecessor);
      measurementConfiguration.setUseSourceInstrumentation(kiekerConfigMixin.isUseSourceInstrumentation());
      measurementConfiguration.setUseCircularQueue(kiekerConfigMixin.isUseCircularQueue());
      measurementConfiguration.setUseSelectiveInstrumentation(kiekerConfigMixin.isUseSelectiveInstrumentation());
      measurementConfiguration.setUseSampling(kiekerConfigMixin.isUseSampling());
      LOG.info("Use source instrumentation: {}", kiekerConfigMixin.isUseSourceInstrumentation());
      return measurementConfiguration;
   }

   public static CauseSearcher getCauseSeacher(final MeasurementConfiguration measurementConfiguration, final JUnitTestTransformer testtransformer,
         final CauseSearcherConfig causeSearcherConfig, final CauseSearchFolders alternateFolders, final BothTreeReader reader) throws IOException, InterruptedException {
      final CauseSearcher tester;
      final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, causeSearcherConfig);
      if (causeSearcherConfig.getRcaStrategy() != null) {
         switch (causeSearcherConfig.getRcaStrategy()) {
         case COMPLETE:
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders);
            break;
         case LEVELWISE:
            tester = new LevelCauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders);
            break;
         case CONSTANT_LEVELS:
            throw new RuntimeException("Measurement for constant count of level currently not supported");
         case UNTIL_SOURCE_CHANGE:
            TreeAnalyzerCreator creatorSource = new TreeAnalyzerCreator() {

               @Override
               public TreeAnalyzer getAnalyzer(BothTreeReader reader, CauseSearcherConfig config) {
                  return new SourceChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor(), config.getPropertyFolder());
               }
            };
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creatorSource);
            break;
         case UNTIL_STRUCTURE_CHANGE:
            TreeAnalyzerCreator creator = new TreeAnalyzerCreator() {

               @Override
               public TreeAnalyzer getAnalyzer(BothTreeReader reader, CauseSearcherConfig config) {
                  return new StructureChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
               }
            };
            tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creator);
            break;
         default:
            throw new RuntimeException("Strategy " + causeSearcherConfig.getRcaStrategy() + " not expected");
         }
      } else {
         LOG.info("Defaulting to StructureCauseSearcher");
         TreeAnalyzerCreator creator = new TreeAnalyzerCreator() {

            @Override
            public TreeAnalyzer getAnalyzer(BothTreeReader reader, CauseSearcherConfig config) {
               return new StructureChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
            }
         };
         tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders, creator);
      }
      return tester;
   }

}
