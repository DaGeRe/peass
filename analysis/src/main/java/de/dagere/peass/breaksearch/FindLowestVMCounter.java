package de.dagere.peass.breaksearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.measurement.TestStatistic;
import de.dagere.peass.analysis.measurement.statistics.MeanCoVData;
import de.dagere.peass.measurement.dataloading.DataAnalyser;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;

public class FindLowestVMCounter extends DataAnalyser {
   private static final Logger LOG = LogManager.getLogger(FindLowestVMCounter.class);
   
   private BufferedWriter writer;

   @Override
   public void processTestdata(final TestData measurementEntry) {
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
            final TestStatistic benchmark = new TestStatistic(entry.getValue());
            boolean alreadyEqual = false;
            TestStatistic minimalStatistic = null;
            int equal = 0;
            for (int i = 3; i < entry.getValue().getCurrent().size(); i++) {
               // entry.getValue().
               final EvaluationPair shortenedPair = new EvaluationPair(entry.getKey(), entry.getValue().getPreviousVersion(), entry.getValue().getTestcase());
               shortenedPair.getCurrent().addAll(entry.getValue().getCurrent().subList(0, i));
               shortenedPair.getPrevius().addAll(entry.getValue().getPrevius().subList(0, i));
               final TestStatistic statistic = new TestStatistic(shortenedPair);
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
