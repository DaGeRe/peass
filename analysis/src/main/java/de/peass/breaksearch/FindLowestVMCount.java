package de.peass.breaksearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.measurement.analysis.DataAnalyser;
import de.peass.measurement.analysis.TestStatistic;
import de.peass.measurement.analysis.statistics.EvaluationPair;
import de.peass.measurement.analysis.statistics.MeanCoVData;
import de.peass.measurement.analysis.statistics.TestData;
import de.peass.utils.OptionConstants;
import de.peran.FolderSearcher;

public class FindLowestVMCount extends DataAnalyser {

   private static final Logger LOG = LogManager.getLogger(FindLowestVMCount.class);

   public static void main(String[] args) throws JAXBException, ParseException, InterruptedException, JsonParseException, JsonMappingException, IOException {
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
      options.addOption(FolderSearcher.DATAOPTION);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      DependencyReaderUtil.loadDependencies(line);

      final FindLowestVMCount flv = new FindLowestVMCount();
      for (int i = 0; i < line.getOptionValues(FolderSearcher.DATA).length; i++) {
         final File folder = new File(line.getOptionValues(FolderSearcher.DATA)[i]);
         LOG.info("Searching in " + folder);
         flv.processDataFolder(folder);
      }
   }

   private BufferedWriter writer;

   @Override
   public void processTestdata(TestData measurementEntry) {
      try {
         final File folder = new File("results/csvs/");
         if (!folder.exists()) {
            folder.mkdirs();
         }
         if (writer == null) {
            writer = new BufferedWriter(new FileWriter(new File(folder, "corr.csv")));
         }
         for (final Map.Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
            final FileWriter tWriter = new FileWriter(new File(folder, entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "#" + measurementEntry.getTestMethod()+".csv"));
            final TestStatistic benchmark = new TestStatistic(entry.getValue(), null);
            boolean alreadyEqual = false;
            TestStatistic minimalStatistic = null;
            int equal = 0;
            for (int i = 3; i < entry.getValue().getCurrent().size(); i++) {
               // entry.getValue().
               final EvaluationPair shortenedPair = new EvaluationPair(entry.getKey(), entry.getValue().getPreviousVersion(), entry.getValue().getTestcase());
               shortenedPair.getCurrent().addAll(entry.getValue().getCurrent().subList(0, i));
               shortenedPair.getPrevius().addAll(entry.getValue().getPrevius().subList(0, i));
               final TestStatistic statistic = new TestStatistic(shortenedPair, null);
               tWriter.write(MeanCoVData.FORMAT.format(statistic.getTValue()) + ";" + getMeanDevString(statistic) + "\n");
               tWriter.flush();
               if (!alreadyEqual) {
                  if (statistic.isChange() == benchmark.isChange()) {
                     equal = i;
                     alreadyEqual = true;
                     minimalStatistic = statistic;
                  }
               } else {
                  if (statistic.isChange() != benchmark.isChange()) {
                     equal = 0;
                     alreadyEqual = false;
                  }
               }
            }
            tWriter.close();
            LOG.info("First maximal equal: {} Executed: {} Change: {}", equal, entry.getValue().getCurrent().size(), benchmark.isChange());
            LOG.info("Statistic data: {}", minimalStatistic);

            writer.write(MeanCoVData.FORMAT.format(minimalStatistic.getTValue()) + ";" + getMeanDevString(minimalStatistic) + "\n");
            writer.flush();
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private String getMeanDevString(final TestStatistic statistic) {
      return MeanCoVData.FORMAT.format(statistic.getCurrentStatistic().getMean()) + ";"
            + MeanCoVData.FORMAT.format(statistic.getCurrentStatistic().getStandardDeviation() / statistic.getCurrentStatistic().getMean()) + ";"
            + MeanCoVData.FORMAT.format(statistic.getPreviousStatistic().getMean()) + ";"
            + MeanCoVData.FORMAT.format(statistic.getPreviousStatistic().getStandardDeviation() / statistic.getPreviousStatistic().getMean());
   }
}
