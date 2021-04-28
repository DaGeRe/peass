package de.dagere.peass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.analysis.VersionSorter;
import de.dagere.peass.utils.RunCommandWriterRCA;
import de.dagere.peass.utils.RunCommandWriterSlurmRCA;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "getchanges", description = "Determines changes based on measurement values using agnostic t-test", mixinStandardHelpOptions = true)
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

   @Option(names = { "-type1error", "--type1error" }, description = "Type 1 error of agnostic-t-test, i.e. probability of considering measurements equal when they are unequal")
   public double type1error = 0.001;

   @Option(names = { "-type2error", "--type2error" }, description = "Type 2 error of agnostic-t-test, i.e. probability of considering measurements unequal when they are equal")
   private double type2error = 0.001;

   public GetChanges() {

   }

   public static void main(final String[] args) {
      final CommandLine commandLine = new CommandLine(new GetChanges());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      VersionSorter.getVersionOrder(dependencyFile, executionFile);

      if (!out.exists()) {
         out.mkdirs();
      }
      final File statisticFolder = new File(out, "statistics");
      if (!statisticFolder.exists()) {
         statisticFolder.mkdir();
      }

      LOG.info("Errors: 1: {} 2: {}", type1error, type2error);

      final ChangeReader reader = createReader(statisticFolder);

      for (final File dataFile : data) {
         reader.readFile(dataFile);
      }
      return null;
   }

   private ChangeReader createReader(final File statisticFolder) throws FileNotFoundException {
      RunCommandWriterRCA runCommandWriter = null;
      RunCommandWriterSlurmRCA runCommandWriterSlurm = null;
      if (VersionSorter.executionData != null) {
         if (VersionComparator.getDependencies().getUrl() != null && !VersionComparator.getDependencies().getUrl().isEmpty()) {
            final PrintStream runCommandPrinter = new PrintStream(new File(statisticFolder, "run-rca-" + VersionComparator.getProjectName() + ".sh"));
            runCommandWriter = new RunCommandWriterRCA(runCommandPrinter, "default", VersionComparator.getDependencies());
            final PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticFolder, "run-rca-slurm-" + VersionComparator.getProjectName() + ".sh"));
            runCommandWriterSlurm = new RunCommandWriterSlurmRCA(runCommandPrinterRCA, "default", VersionComparator.getDependencies());
         }
      }

      final ChangeReader reader = new ChangeReader(statisticFolder, runCommandWriter, runCommandWriterSlurm);
      reader.setType1error(type1error);
      reader.setType2error(type2error);
      return reader;
   }
   
}
