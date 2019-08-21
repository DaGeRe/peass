package de.peass;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.confidence.KoPeMeDataHelper;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Tells whether a change happened in a data folder", name = "ischange")
public class IsChange implements Callable<Integer> {
   
   private static final Logger LOG = LogManager.getLogger(IsChange.class);

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   private File dataFolder;

   @Option(names = { "-type1error", "--type1error" }, description = "Type 1 error of agnostic-t-test, i.e. probability of considering measurements equal when they are unequal")
   private double type1error = 0.01;

   @Option(names = { "-type2error", "--type2error" }, description = "Type 2 error of agnostic-t-test, i.e. probability of considering measurements unequal when they are equal")
   private double type2error = 0.01;

   public static void main(final String[] args) {
      final IsChange command = new IsChange();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   @Override
   public Integer call() throws Exception {
      final File xmlFile = dataFolder.listFiles((FilenameFilter) new WildcardFileFilter("*.xml"))[0];

      final Kopemedata data = new XMLDataLoader(xmlFile).getFullData();
      
      final Chunk chunk = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getChunk().get(0);

      final String[] versions = KoPeMeDataHelper.getVersions(chunk);
      LOG.debug(versions[1]);
      final DescribedChunk describedChunk = new DescribedChunk(chunk, versions[0], versions[1]);
      final boolean change = describedChunk.getStatistic(type1error, type2error).isChange();
      LOG.info("Change: {}", change);
      // TODO Auto-generated method stub
      return change ? 1 : 0;
   }
}
