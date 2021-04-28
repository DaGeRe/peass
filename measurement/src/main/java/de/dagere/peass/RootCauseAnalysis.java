package de.dagere.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.analyzer.SourceChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.analyzer.StructureChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.analyzer.TreeAnalyzer;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.measurement.rca.searcher.CauseSearcherComplete;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.dagere.peass.measurement.rca.searcher.TreeAnalyzerCreator;
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
      measurementConfiguration.setVersion(version);
      measurementConfiguration.setVersionOld(predecessor);
      measurementConfiguration.setUseSourceInstrumentation(kiekerConfigMixin.isUseSourceInstrumentation());
      measurementConfiguration.setEnableAdaptiveConfig(!kiekerConfigMixin.isUseSourceInstrumentation());
      measurementConfiguration.setUseCircularQueue(kiekerConfigMixin.isUseCircularQueue());
      if (kiekerConfigMixin.isUseSourceInstrumentation() && kiekerConfigMixin.isNotUseSelectiveInstrumentation()) {
         measurementConfiguration.setUseSelectiveInstrumentation(false);
      } else {
         measurementConfiguration.setUseSelectiveInstrumentation(true);
      }

      measurementConfiguration.setUseSampling(kiekerConfigMixin.isUseSampling());
      LOG.info("Use source instrumentation: {}", kiekerConfigMixin.isUseSourceInstrumentation());
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
         case CONSTANT_LEVELS:
            throw new RuntimeException("Measurement for constant count of level currently not supported");
         case UNTIL_SOURCE_CHANGE:
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
