package de.dagere.peass;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.measurement.dataloading.KoPeMeDataHelper;
import de.dagere.peass.measurement.statistics.data.DescribedChunk;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(description = "Tells whether a change happened in a data folder", name = "ischange")
public class IsChangeStarter implements Callable<Integer> {

   private static final Logger LOG = LogManager.getLogger(IsChangeStarter.class);

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   private File dataFolder;

   @Mixin
   protected StatisticsConfigMixin statisticConfigMixin;

   public static void main(final String[] args) {
      final IsChangeStarter command = new IsChangeStarter();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   @Override
   public Integer call() throws Exception {
      LOG.debug("Checking: {}", dataFolder);
      final File xmlFile = dataFolder.listFiles((FilenameFilter) new WildcardFileFilter("*.json"))[0];

      final Kopemedata data = JSONDataLoader.loadData(xmlFile);

      final VMResultChunk chunk = data.getFirstMethodResult().getDatacollectorResults().get(0).getChunks().get(0);

      final String[] versions = KoPeMeDataHelper.getVersions(chunk);
      LOG.debug(versions[1]);
      final DescribedChunk describedChunk = new DescribedChunk(chunk, versions[0], versions[1]);
      final boolean change = describedChunk.getStatistic(statisticConfigMixin.getStasticsConfig()).isChange();
      LOG.info("Change: {}", change);
      // TODO Auto-generated method stub
      return change ? 1 : 0;
   }
}
