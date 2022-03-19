package de.dagere.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.dataloading.VersionSorter;
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

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionFile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   @Option(names = { "-out", "--out" }, description = "Path for saving the changefile")
   private File out = new File("results");

   @Mixin
   protected StatisticsConfigMixin statisticConfigMixin;

   public GetChangesStarter() {

   }

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new GetChangesStarter());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      SelectedTests selectedTests = VersionSorter.getSelectedTests(staticSelectionFile, executionFile);

      if (!out.exists()) {
         out.mkdirs();
      }

      LOG.info("Errors: 1: {} 2: {}", statisticConfigMixin.getType1error(), statisticConfigMixin.getType2error());

      ResultsFolders folders = new ResultsFolders(out, "out");
      final ChangeReader reader = createReader(folders, selectedTests);

      if (staticSelectionFile != null) {
         StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
         reader.setTests(dependencies.toExecutionData().getVersions());

      }
      if (executionFile != null) {
         ExecutionData executions = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
         reader.setTests(executions.getVersions());
      }

      for (final File dataFile : data) {
         reader.readFile(dataFile);
      }
      return null;
   }

   private ChangeReader createReader(final ResultsFolders resultsFolders, final SelectedTests selectedTests) throws FileNotFoundException {
      RunCommandWriterRCA runCommandWriter = null;
      RunCommandWriterSlurmRCA runCommandWriterSlurm = null;
      if (selectedTests.getUrl() != null && !selectedTests.getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(resultsFolders.getStatisticsFile().getParentFile(), "run-rca-" + selectedTests.getName() + ".sh"));
         runCommandWriter = new RunCommandWriterRCA(runCommandPrinter, "default", selectedTests);
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(resultsFolders.getStatisticsFile().getParentFile(), "run-rca-slurm-" + selectedTests.getName() + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(runCommandPrinterRCA, "default", selectedTests);
      }
     
      final ChangeReader reader = new ChangeReader(resultsFolders, runCommandWriter, runCommandWriterSlurm, selectedTests);
      reader.setConfig(statisticConfigMixin.getStasticsConfig());
      return reader;
   }

}
