package de.peass.measurement.analysis;

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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.generated.Result;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.analysis.DataAnalyser;
import de.dagere.peass.measurement.analysis.Relation;
import de.dagere.peass.measurement.analysis.statistics.EvaluationPair;
import de.dagere.peass.measurement.analysis.statistics.TestData;
import de.dagere.peass.statistics.ConfidenceIntervalInterpretion;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.StreamGobbler;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.measurement.analysis.statistics.MeanCoVData;
import de.peass.measurement.analysis.statistics.MeanHistogramData;
import de.peran.AnalyseOneTest;
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

   private final File changeFile;
   private final ProjectChanges projectChanges = new ProjectChanges();
   private final ModuleClassMapping mapping;

   private final ProjectStatistics info;

   public AnalyseFullData(final ProjectStatistics info) {
      this(new File(AnalyseOneTest.RESULTFOLDER, "changes.json"), info, null);
   }

   public AnalyseFullData(final File changesFile, final ProjectStatistics info, final ModuleClassMapping mapping) {
      this.changeFile = changesFile;
      this.mapping = mapping;
      this.info = info;
      LOG.info("Writing changes to: {}", changeFile.getAbsolutePath());
      try {
         FolderSearcher.MAPPER.writeValue(changeFile, projectChanges);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public ProjectChanges getProjectChanges() {
      return projectChanges;
   }

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> versionEntry : measurementEntry.getMeasurements().entrySet()) {
         final String version = versionEntry.getKey();
         LOG.debug("Analysing: {} ({}#{}) Complete: {}", version, measurementEntry.getTestClass(), measurementEntry.getTestMethod(), versionEntry.getValue().isComplete());

         final TestStatistic teststatistic = new TestStatistic(versionEntry.getValue(), info);

         if (Constants.DRAW_RESULTS) {
            drawPNGs(measurementEntry, versionEntry, version, teststatistic);
         }

         LOG.debug("Change: {} T: {}", teststatistic.isChange(), teststatistic.getTValue());

         if (teststatistic.isChange()) {
            addChangeData(measurementEntry, versionEntry, version, teststatistic);

            try {
               FolderSearcher.MAPPER.writeValue(changeFile, projectChanges);
            } catch (final IOException e) {
               e.printStackTrace();
            }

         }
      }
      versions.addAll(measurementEntry.getMeasurements().keySet());
      LOG.debug("Version: {}", measurementEntry.getMeasurements().keySet());
      testcases += measurementEntry.getMeasurements().size();
   }

   private void drawPNGs(final TestData measurementEntry, final Entry<String, EvaluationPair> versionEntry, final String version, final TestStatistic teststatistic) {
      final File resultFile = generatePlots(measurementEntry, versionEntry, teststatistic.isChange());
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

   private void addChangeData(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final String version, final TestStatistic teststatistic) {
      Relation tRelation = teststatistic.getConfidenceResult();
      if (teststatistic.getDiff() > 0) {
         tRelation = Relation.LESS_THAN;
      } else {
         tRelation = Relation.GREATER_THAN;
      }

      long repetitions = entry.getValue().getCurrent().get(0).getRepetitions();

      final double diffPercent = ((double) teststatistic.getDiff()) / 100;
      final double mean = teststatistic.getPreviousStatistic().getMean() / repetitions;

      final TestCase currentTest = getCurrentTestcase(measurementEntry);

      projectChanges.addChange(currentTest, version,
            teststatistic.getConfidenceResult(), tRelation, mean,
            diffPercent, teststatistic.getTValue(),
            teststatistic.getCurrentStatistic().getN());
      projectChanges.setVersionCount(versions.size());
      projectChanges.setTestcaseCount(testcases);

      LOG.info("Version: {} vs {} Klasse: {}#{}", version, entry.getValue().getPreviousVersion(), measurementEntry.getTestClass(),
            measurementEntry.getTestMethod());
      LOG.debug("Confidence Interval Comparison: {}", teststatistic.getConfidenceResult());

      System.out.println("git diff " + version + ".." + VersionComparator.getPreviousVersion(version));
   }

   private TestCase getCurrentTestcase(final TestData measurementEntry) {
      final TestCase currentTest;
      if (mapping != null) {
         final String module = mapping.getModuleOfClass(measurementEntry.getTestClass());
         currentTest = new TestCase(measurementEntry.getTestClass(), measurementEntry.getTestMethod(), module);
      } else {
         currentTest = measurementEntry.getTestCase();
      }
      return currentTest;
   }

   private File generatePlots(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final boolean change) {
      // final List<Result> currentValues = ConfidenceInterval.getWarmupData(entry.getValue().getCurrent());
      // final List<Result> previousValues = ConfidenceInterval.getWarmupData(entry.getValue().getPrevius());
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
      StreamGobbler.showFullProcess(p2);
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

   public static void main(final String[] args) throws InterruptedException, JAXBException, JsonParseException, JsonMappingException, IOException {
      final File folder = new File(args[0]);
      if (!folder.getName().equals("measurements")) {
         throw new RuntimeException("Can only be executed with measurements-folder! For searching folders, use FolderSearcher");
      }
      LOG.info("Draw results: " + Constants.DRAW_RESULTS);
      final File dependencyFile = new File(args[1]);
      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
      VersionComparator.setDependencies(dependencies);
      final AnalyseFullData analyseFullData = new AnalyseFullData(new ProjectStatistics());
      analyseFullData.analyseFolder(folder);

   }

   public int getChanges() {
      return projectChanges.getChangeCount();
   }
}
