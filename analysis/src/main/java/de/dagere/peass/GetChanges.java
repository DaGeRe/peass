package de.dagere.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.measurement.dataloading.VersionSorter;
import de.dagere.peass.measurement.utils.RunCommandWriterRCA;
import de.dagere.peass.measurement.utils.RunCommandWriterSlurmRCA;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "getchanges", description = "Determines changes based on measurement values using the specified statistical test", mixinStandardHelpOptions = true)
public class GetChanges implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(GetChanges.class);

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionFile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   @Option(names = { "-out", "--out" }, description = "Path for saving the changefile")
   private File out = new File("results");

   @Mixin
   protected StatisticsConfigMixin statisticConfigMixin;

   public GetChanges() {

   }

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new GetChanges());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      SelectedTests selectedTests = VersionSorter.getSelectedTests(dependencyFile, executionFile);

      if (!out.exists()) {
         out.mkdirs();
      }
      final File statisticFolder = new File(out, "statistics");
      if (!statisticFolder.exists()) {
         statisticFolder.mkdir();
      }

      LOG.info("Errors: 1: {} 2: {}", statisticConfigMixin.getType1error(), statisticConfigMixin.getType2error());

      final ChangeReader reader = createReader(statisticFolder, selectedTests);

      if (dependencyFile != null) {
         Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
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

   private ChangeReader createReader(final File statisticFolder, final SelectedTests selectedTests) throws FileNotFoundException {
      RunCommandWriterRCA runCommandWriter = null;
      RunCommandWriterSlurmRCA runCommandWriterSlurm = null;
      if (selectedTests.getUrl() != null && !selectedTests.getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(statisticFolder, "run-rca-" + selectedTests.getName() + ".sh"));
         runCommandWriter = new RunCommandWriterRCA(runCommandPrinter, "default", selectedTests);
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticFolder, "run-rca-slurm-" + selectedTests.getName() + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(runCommandPrinterRCA, "default", selectedTests);
      }

      final ChangeReader reader = new ChangeReader(statisticFolder, runCommandWriter, runCommandWriterSlurm, selectedTests);
      reader.setConfig(statisticConfigMixin.getStasticsConfig());
      return reader;
   }

}
