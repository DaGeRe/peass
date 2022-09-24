package de.dagere.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.dataloading.CommitSorter;
import de.dagere.peass.measurement.utils.RunCommandWriterRCA;
import de.dagere.peass.measurement.utils.RunCommandWriterSlurmRCA;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "getchanges", description = "Determines changes based on measurement values using the specified statistical test", mixinStandardHelpOptions = true)
public class GetChangesStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GetChangesStarter.class);

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the static selection file")
   protected File staticSelectionFile;

   @Option(names = { "-executionFile", "--executionFile" }, description = "Path to the executionFile")
   protected File executionFile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   @Option(names = { "-out", "--out" }, description = "Path for saving the changefile")
   private File out = new File("results");

   @Mixin
   ExecutionConfigMixin executionMixin;
   
   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;
   
   @Mixin
   KiekerConfigMixin kiekerConfigMixin;
   
   @Mixin
   private StatisticsConfigMixin statisticConfigMixin;

   public GetChangesStarter() {

   }

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new GetChangesStarter());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      SelectedTests selectedTests = CommitSorter.getSelectedTests(staticSelectionFile, executionFile);

      if (!out.exists()) {
         out.mkdirs();
      }

      LOG.info("Errors: 1: {} 2: {}", statisticConfigMixin.getType1error(), statisticConfigMixin.getType2error());

      ResultsFolders folders = new ResultsFolders(out, "out");
      
      MeasurementConfig config = new MeasurementConfig(measurementConfigMixin, executionMixin, statisticConfigMixin, kiekerConfigMixin);
      final ChangeReader reader = createReader(folders, selectedTests, config);

      if (staticSelectionFile != null) {
         StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
         reader.setTests(dependencies.toExecutionData().getCommits());

      }
      if (executionFile != null) {
         ExecutionData executions = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
         reader.setTests(executions.getCommits());
      }

      for (final File dataFile : data) {
         if (!dataFile.exists()) {
            throw new RuntimeException("File " + dataFile + " does not exist!");
         }
         reader.readFolder(dataFile);
      }
      return null;
   }

   private ChangeReader createReader(final ResultsFolders resultsFolders, final SelectedTests selectedTests, MeasurementConfig config) throws FileNotFoundException {
      RunCommandWriterRCA runCommandWriter = null;
      RunCommandWriterSlurmRCA runCommandWriterSlurm = null;
      if (selectedTests.getUrl() != null && !selectedTests.getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(resultsFolders.getStatisticsFile().getParentFile(), "run-rca-" + selectedTests.getName() + ".sh"));
         runCommandWriter = new RunCommandWriterRCA(config, runCommandPrinter, "default", selectedTests);
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(resultsFolders.getStatisticsFile().getParentFile(), "run-rca-slurm-" + selectedTests.getName() + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(config, runCommandPrinterRCA, "default", selectedTests);
      }
     
      final ChangeReader reader = new ChangeReader(resultsFolders, runCommandWriter, runCommandWriterSlurm, selectedTests, config.getStatisticsConfig());
      return reader;
   }

}
