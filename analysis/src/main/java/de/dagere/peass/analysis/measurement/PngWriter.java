package de.dagere.peass.analysis.measurement;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.analysis.measurement.statistics.MeanCoVData;
import de.dagere.peass.analysis.measurement.statistics.MeanHistogramData;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;
import de.dagere.peass.utils.StreamGobbler;

//TODO Extracted for readability, functionality not tested
public class PngWriter {
   
   private final File changeFile;
   
   public PngWriter(final File changeFile) {
      this.changeFile = changeFile;
   }

   public void drawPNGs(final TestData measurementEntry, final Entry<String, EvaluationPair> versionEntry, final String version, final TestStatistic teststatistic) {
      final File resultFile = generatePlots(measurementEntry, versionEntry, teststatistic.isChange());
      final File stuffFolder;
      if (teststatistic.isChange()) {
         stuffFolder = new File(changeFile.getParentFile(), "graphs/results/change");
      } else {
         stuffFolder = new File(changeFile.getParentFile(), "graphs/results/nochange");
      }
      try {
         FileUtils.copyFile(resultFile, new File(stuffFolder, version + "_" + measurementEntry.getTestMethod() + ".png"));
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
   
   public File generatePlots(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final boolean change) {
      // final List<Result> currentValues = ConfidenceInterval.getWarmupData(entry.getValue().getCurrent());
      // final List<Result> previousValues = ConfidenceInterval.getWarmupData(entry.getValue().getPrevius());
      final List<VMResult> currentValues = entry.getValue().getCurrent();
      final List<VMResult> previousValues = entry.getValue().getPrevius();

      final MeanCoVData data = new MeanCoVData(measurementEntry.getTestMethod(), currentValues);
      final MeanCoVData dataPrev = new MeanCoVData(measurementEntry.getTestMethod(), previousValues);

      final MeanHistogramData histData = new MeanHistogramData(currentValues);
      final MeanHistogramData histDataPrev = new MeanHistogramData(previousValues);

      final File folder = new File(changeFile.getParentFile(),
            "graphs" + File.separator + entry.getKey() + File.separator + measurementEntry.getTestClass() + File.separator + measurementEntry.getTestMethod());
      if (!folder.exists()) {
         folder.mkdirs();
      }
      final File multmimodal = new File(changeFile.getParentFile(), "graphs/multimodal");
      final File multmimodalChange = new File(changeFile.getParentFile(), "graphs/multimodal/change");
      final File unimodal = new File(changeFile.getParentFile(), "graphs/unimodal");
      final File unimodalChange = new File(changeFile.getParentFile(), "graphs/unimodal/change/");
      for (final File file : new File[] { multmimodal, multmimodalChange, unimodal, unimodalChange }) {
         if (!file.exists()) {
            file.mkdirs();
         }
      }

      try {
         createGraphs(measurementEntry, entry, change, data, dataPrev, histData, histDataPrev, folder, multmimodal, multmimodalChange, unimodal, unimodalChange);

         return new File(folder, "graph.png");
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   
   public void createGraphs(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final boolean change, final MeanCoVData data, final MeanCoVData dataPrev,
         final MeanHistogramData histData, final MeanHistogramData histDataPrev, final File folder, final File multmimodal, final File multmimodalChange, final File unimodal,
         final File unimodalChange) throws IOException {
      histData.printHistData(new File(folder, "current_hist.csv"));
      histDataPrev.printHistData(new File(folder, "prev_hist.csv"));

      data.printAverages(new File(folder, "current.csv"));
      dataPrev.printAverages(new File(folder, "prev.csv"));

      final double binwidth = histData.getSpan() / 10;

      final String init = "set datafile separator ';';set decimalsign locale; set decimalsign \",\";set term png;";

      executeGnuplot(folder, "gnuplot", "-e",
            init + "set output 'graph.png'; plot 'current.csv' u ($0*" + data.getAvgCount() + "):1, 'prev.csv' u ($0*" + data.getAvgCount() + "):1");
      executeGnuplot(folder, "gnuplot", "-e", init
            + "set output 'histogram.png'; binwidth=" + binwidth + "; set boxwidth binwidth;"
            + "bin(x,width)=width*floor(x/width); plot 'current_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes, 'prev_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes");
      executeGnuplot(folder, "gnuplot", "-e", init
            + "set output 'histogram_first.png'; binwidth=" + binwidth + "; set boxwidth binwidth;"
            + "bin(x,width)=width*floor(x/width); plot 'current_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes");
      executeGnuplot(folder, "gnuplot", "-e", init
            + "set output 'histogram_second.png'; binwidth=" + ((histDataPrev.getSpan() / 10)) + "; set boxwidth binwidth;"
            + "bin(x,width)=width*floor(x/width); plot 'prev_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes");

      final String filename = entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod() + ".png";
      final String name = entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod() + "_1.png";
      final String name2 = entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod() + "_2.png";
      if (MultimodalUtil.isRInstalled()) {
         if (MultimodalUtil.isMultimodalSilverman(entry.getValue().getCurrent()) || MultimodalUtil.isMultimodalSilverman(entry.getValue().getPrevius())) {
            if (change) {
               FileUtils.copyFile(new File(folder, "histogram.png"), new File(multmimodalChange, filename));
               FileUtils.copyFile(new File(folder, "histogram_first.png"), new File(multmimodalChange, name));
               FileUtils.copyFile(new File(folder, "histogram_second.png"), new File(multmimodalChange, name2));
            } else {
               FileUtils.copyFile(new File(folder, "histogram.png"), new File(multmimodal, filename));
               FileUtils.copyFile(new File(folder, "histogram_first.png"), new File(multmimodal, name));
               FileUtils.copyFile(new File(folder, "histogram_second.png"), new File(multmimodal, name2));
            }
         } else {
            if (change) {
               FileUtils.copyFile(new File(folder, "histogram.png"), new File(unimodalChange, filename));
               FileUtils.copyFile(new File(folder, "histogram_first.png"), new File(unimodalChange, name));
               FileUtils.copyFile(new File(folder, "histogram_second.png"), new File(unimodalChange, name2));
            } else {
               FileUtils.copyFile(new File(folder, "histogram.png"), new File(unimodal, filename));
               FileUtils.copyFile(new File(folder, "histogram_first.png"), new File(unimodal, name));
               FileUtils.copyFile(new File(folder, "histogram_second.png"), new File(unimodal, name2));
            }
         }
      }
   }

   private void executeGnuplot(final File folder, final String... command) throws IOException {
      final ProcessBuilder pb2 = new ProcessBuilder(command);
      pb2.directory(folder);
      final Process p2 = pb2.start();
      StreamGobbler.showFullProcess(p2);
   }
}
