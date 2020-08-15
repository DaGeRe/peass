package de.peran.measurement.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Result;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.DataAnalyser;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.statistics.EvaluationPair;
import de.peass.measurement.analysis.statistics.MeanCoVData;
import de.peass.measurement.analysis.statistics.MeanHistogramData;
import de.peass.measurement.analysis.statistics.TestData;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peran.AnalyseOneTest;
import de.peran.Environment;
import de.peran.FolderSearcher;

/**
 * Analyzes full data and tells which version contain changes based upon given statistical tests (confidence interval, MannWhitney, ..)
 * 
 * @author reichelt
 *
 */
public class AnalyseFullData extends DataAnalyser {

   private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

   public Set<String> versions = new HashSet<>();
   public int testcases = 0;

   private final File changeKnowledgeFile;
   public final ProjectChanges knowledge = new ProjectChanges();
//   public final VersionKnowledge oldKnowledge;

   private ProjectStatistics info;

   public AnalyseFullData(final ProjectStatistics info) {
      this(new File(AnalyseOneTest.RESULTFOLDER, "changes.json"));
      this.info = info;
   }

   public AnalyseFullData(final File knowledgeFile) {
      this.changeKnowledgeFile = knowledgeFile;
//      oldKnowledge = VersionKnowledge.getOldChanges();
      LOG.info("Writing changes to: {}", changeKnowledgeFile.getAbsolutePath());
      try {
         FolderSearcher.MAPPER.writeValue(changeKnowledgeFile, knowledge);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         final String version = entry.getKey();
         LOG.debug("Analysing: {} ({}#{}) Complete: {}", version, measurementEntry.getTestClass(), measurementEntry.getTestMethod(), entry.getValue().isComplete());
         final TestStatistic teststatistic = new TestStatistic(entry.getValue(), info);

         if (Environment.DRAW_RESULTS) {
            final File resultFile = generatePlots(measurementEntry, entry, teststatistic.isChange());
            final File stuffFolder;
            if (teststatistic.isChange()) {
               stuffFolder = new File(AnalyseOneTest.RESULTFOLDER, "graphs/results/change");
            } else {
               stuffFolder = new File(AnalyseOneTest.RESULTFOLDER, "graphs/results/nochange");
            }
            try {
               FileUtils.copyFile(resultFile, new File(stuffFolder, version + "_" + measurementEntry.getTestMethod() + ".png"));
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }

         LOG.debug("Change: {} T: {}", teststatistic.isChange(), teststatistic.getTValue());

         // if (teststatistic.getConfidenceResult() != Relation.EQUAL) {
         if (teststatistic.isChange()) {
            Relation tRelation = teststatistic.getConfidenceResult();
            if (teststatistic.getDiff() > 0) {
               tRelation = Relation.LESS_THAN;
            } else {
               tRelation = Relation.GREATER_THAN;
            }

            final double diffPercent = ((double) teststatistic.getDiff()) / 100;
            knowledge.addChange(measurementEntry.getTestCase(), version,
                  teststatistic.getConfidenceResult(), tRelation, teststatistic.getPreviousStatistic().getMean(), 
                  diffPercent, teststatistic.getTValue(),
                  teststatistic.getCurrentStatistic().getN());
            knowledge.setVersionCount(versions.size());
            knowledge.setTestcaseCount(testcases);

            try {
               FolderSearcher.MAPPER.writeValue(changeKnowledgeFile, knowledge);
            } catch (final IOException e) {
               e.printStackTrace();
            }
            // csvResultWriter.write(version + ";" + "vim " + viewName + ";" + measurementEntry.getTestCase().getTe + ";" + tTestResult + ";" + confidenceResult + "\n");
            // csvResultWriter.flush();

            LOG.info("Version: {} vs {} Klasse: {}#{}", version, entry.getValue().getPreviousVersion(), measurementEntry.getTestClass(),
                  measurementEntry.getTestMethod());
            LOG.debug("Confidence Interval Comparison: {}", teststatistic.getConfidenceResult());

            System.out.println("git diff " + version + ".." + VersionComparator.getPreviousVersion(version));
         }
      }
      versions.addAll(measurementEntry.getMeasurements().keySet());
      LOG.debug("Version: {}", measurementEntry.getMeasurements().keySet());
      testcases += measurementEntry.getMeasurements().size();
   }

   private File generatePlots(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final boolean change) {
//      final List<Result> currentValues = ConfidenceInterval.getWarmupData(entry.getValue().getCurrent());
//      final List<Result> previousValues = ConfidenceInterval.getWarmupData(entry.getValue().getPrevius());
      final List<Result> currentValues = entry.getValue().getCurrent();
      final List<Result> previousValues = entry.getValue().getPrevius();

      final MeanCoVData data = new MeanCoVData(measurementEntry.getTestMethod(), currentValues);
      final MeanCoVData dataPrev = new MeanCoVData(measurementEntry.getTestMethod(), previousValues);

      final MeanHistogramData histData = new MeanHistogramData(currentValues);
      final MeanHistogramData histDataPrev = new MeanHistogramData(previousValues);

      final File folder = new File(AnalyseOneTest.RESULTFOLDER,
            "graphs" + File.separator + entry.getKey() + File.separator + measurementEntry.getTestClass() + File.separator + measurementEntry.getTestMethod());
      if (!folder.exists()) {
         folder.mkdirs();
      }
      final File multmimodal = new File(AnalyseOneTest.RESULTFOLDER, "graphs/multimodal");
      final File multmimodalChange = new File(AnalyseOneTest.RESULTFOLDER, "graphs/multimodal/change");
      final File unimodal = new File(AnalyseOneTest.RESULTFOLDER, "graphs/unimodal");
      final File unimodalChange = new File(AnalyseOneTest.RESULTFOLDER, "graphs/unimodal/change/");
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
      de.peass.utils.StreamGobbler.showFullProcess(p2);
   }

   private static void removeOutliers(final List<Result> previus) {
      final DescriptiveStatistics statistics1 = ConfidenceIntervalInterpretion.getStatistics(previus);
      for (final Iterator<Result> result = previus.iterator(); result.hasNext();) {
         final Result r = result.next();
         final double diff = Math.abs(r.getValue() - statistics1.getPercentile(50));
         final double z = diff / statistics1.getStandardDeviation();
         LOG.debug("Val: {} Z: {} Remove: {}", r.getValue(), z, z > 3);
         if (z > 3) {
            result.remove();
         }
      }
   }

   public static void main(final String[] args) throws InterruptedException, JAXBException {
      final File folder = new File(args[0]);
      if (!folder.getName().equals("measurements")) {
         throw new RuntimeException("Can only be executed with measurements-folder! For searching folders, use FolderSearcher");
      }
      LOG.info("Draw results: " + Environment.DRAW_RESULTS);
      final File dependencyFile = new File(args[1]);
      final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
      VersionComparator.setDependencies(dependencies);
      final AnalyseFullData analyseFullData = new AnalyseFullData(new ProjectStatistics());
      analyseFullData.analyseFolder(folder);

   }

   public int getChanges() {
      return knowledge.getChangeCount();
   }
}
