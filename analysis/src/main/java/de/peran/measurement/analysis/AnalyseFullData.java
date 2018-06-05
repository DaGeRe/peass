package de.peran.measurement.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;
import de.peran.AnalyseOneTest;
import de.peran.analysis.helper.MinimalExecutionDeterminer;
import de.peran.analysis.knowledge.Change;
import de.peran.analysis.knowledge.Changes;
import de.peran.analysis.knowledge.VersionKnowledge;
import de.peran.dependencyprocessors.VersionComparator;
import de.peran.measurement.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peran.measurement.analysis.statistics.EvaluationPair;
import de.peran.measurement.analysis.statistics.MeanCoVData;
import de.peran.measurement.analysis.statistics.MeanHistData;
import de.peran.measurement.analysis.statistics.Relation;
import de.peran.measurement.analysis.statistics.TestData;

/**
 * Analyzes full data and tells which version contain changes based upon given statistical tests (confidence interval, MannWhitney, ..)
 * 
 * @author reichelt
 *
 */
public class AnalyseFullData extends DataAnalyser {

   private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

   public static Set<String> versions = new HashSet<>();
   public static int testcases = 0, changes = 0;

   private static File myFile = new File("results_summary.csv");
   private static final ObjectMapper MAPPER = new ObjectMapper();
   static {
      try {
         csvResultWriter = new BufferedWriter(new FileWriter(myFile));
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private final File changeKnowledgeFile;
   public final VersionKnowledge knowledge = new VersionKnowledge();
   public final VersionKnowledge oldKnowledge = new VersionKnowledge();

   private static BufferedWriter csvResultWriter;

   public AnalyseFullData() {
      changeKnowledgeFile = new File(AnalyseOneTest.RESULTFOLDER, "changes.json");
      try {
         for (final File potentialKnowledgeFile : AnalyseOneTest.RESULTFOLDER.listFiles()) {
            if (!potentialKnowledgeFile.isDirectory()) {
               final VersionKnowledge knowledge = MAPPER.readValue(potentialKnowledgeFile, VersionKnowledge.class);
               for (final Map.Entry<String, Changes> oldFileEntry : knowledge.getVersionChanges().entrySet()) {
                  final Changes version = oldKnowledge.getVersion(oldFileEntry.getKey());
                  if (version == null) {
                     oldKnowledge.getVersionChanges().put(oldFileEntry.getKey(), oldFileEntry.getValue());
                  } else {
                     for (final Map.Entry<String, List<Change>> versionEntry : oldFileEntry.getValue().getTestcaseChanges().entrySet()) {
                        final List<Change> changes = version.getTestcaseChanges().get(versionEntry.getKey());
                        if (changes == null) {
                           version.getTestcaseChanges().put(versionEntry.getKey(), versionEntry.getValue());
                        } else {
                           for (final Change oldChange : versionEntry.getValue()) {
                              boolean found = false;
                              for (final Change change : changes) {
                                 if (change.getDiff().equals(oldChange.getDiff())) {
                                    found = true;
                                    if (oldChange.getType() != null) {
                                       change.setType(oldChange.getType());
                                    }
                                    if (oldChange.getCorrectness() != null) {
                                       change.setCorrectness(oldChange.getCorrectness());
                                    }
                                 }
                              }
                              if (!found) {
                                 changes.add(oldChange);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public AnalyseFullData(File knowledgeFile) {
      this.changeKnowledgeFile = knowledgeFile;
   }

   private static final double CONFIDENCE = 0.01;

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         final Changes changeList = knowledge.getVersion(entry.getKey());
         LOG.debug("Analysing: {} ({}#{}) Complete: {}", entry.getKey(), measurementEntry.getTestClass(), measurementEntry.getTestMethod(), entry.getValue().isComplete());

         // if (true || entry.getValue().isComplete()) {
         List<Result> previous = entry.getValue().getPrevius();
         List<Result> current = entry.getValue().getCurrent();

         if (previous.size() == 0 || current.size() == 0) {
            LOG.error("Data empty: {} {}", entry.getKey(), measurementEntry.getTestClass());
            if (previous.size() == 0) {
               LOG.error("Previous " + entry.getValue().getPreviousVersion() + " empty");
            }
            if (current.size() == 0) {
               LOG.error("Previous " + entry.getValue().getVersion() + " empty");
            }
            return;
         }
         if (Double.isNaN(current.get(0).getDeviation()) || Double.isNaN(previous.get(0).getDeviation())) {
            LOG.error("Data contained NaN - not handling result");
            return;
         }

         // int warmup, end;
         // if (previus.get(0).getFulldata().getValue().size() == 10000) {
         // warmup = 5000;
         // end = 10000;
         // LOG.debug("Values: {} {}", warmup, end);
         // previus = MinimalExecutionDeterminer.shortenValues(previus, warmup, end);
         // current = MinimalExecutionDeterminer.shortenValues(current, warmup, end);
         // } else {
         previous = MinimalExecutionDeterminer.cutValuesMiddle(previous);
         current = MinimalExecutionDeterminer.cutValuesMiddle(current);
         // }

         final int resultslength = Math.min(previous.size(), current.size());

         LOG.debug("Results: {}", resultslength);

         if (resultslength > 1) {

            removeOutliers(previous);
            removeOutliers(current);
            final DescriptiveStatistics statistics1 = ConfidenceIntervalInterpretion.getStatistics(previous);
            final DescriptiveStatistics statistics2 = ConfidenceIntervalInterpretion.getStatistics(current);

            final List<Double> before_double = MultipleVMTestUtil.getAverages(previous);
            final List<Double> after_double = MultipleVMTestUtil.getAverages(current);

            // final List<Result> prevResults = previus.subList(0, resultslength);
            // final List<Result> currentResults = current.subList(0, resultslength);
            final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(previous, current);
            // final Relation anovaResult = ANOVATestWrapper.compare(prevResults, currentResults);

            final DescriptiveStatistics ds = new DescriptiveStatistics(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])));
            final DescriptiveStatistics ds2 = new DescriptiveStatistics(ArrayUtils.toPrimitive(after_double.toArray(new Double[0])));
            LOG.debug(ds.getMean() + " " + ds2.getMean() + " " + ds.getStandardDeviation() + " " + ds2.getStandardDeviation());

            final double tValue = TestUtils.t(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])), ArrayUtils.toPrimitive(after_double.toArray(new Double[0])));
            final boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before_double.toArray(new Double[0])), ArrayUtils.toPrimitive(after_double.toArray(new Double[0])),
                  CONFIDENCE);

            final int diff = (int) (((statistics1.getMean() - statistics2.getMean()) * 10000) / statistics1.getMean());
            // double anovaDeviation = ANOVATestWrapper.getANOVADeviation(prevResults, currentResults);
            LOG.debug("Means: {} {} Diff: {} % T-Value: {} Change: {}", statistics1.getMean(), statistics2.getMean(), ((double) diff) / 100, tValue, change);

            final File resultFile = generatePlots(measurementEntry, entry, change);
            final File stuffFolder;
            if (change) {
               stuffFolder = new File(AnalyseOneTest.RESULTFOLDER, "graphs/results/change");
            } else {
               stuffFolder = new File(AnalyseOneTest.RESULTFOLDER, "graphs/results/nochange");
            }
            try {
               FileUtils.copyFile(resultFile, new File(stuffFolder, entry.getKey() + "_" + measurementEntry.getTestMethod() + ".png"));
            } catch (final IOException e) {
               e.printStackTrace();
            }

            if (change) {
               Relation tRelation;
               if (diff > 0) {
                  tRelation = Relation.LESS_THAN;
               } else {
                  tRelation = Relation.GREATER_THAN;
               }
               changes++;
               final String viewName = "view_" + entry.getKey() + "/diffs/" + measurementEntry.getTestMethod() + ".txt";
               updateKnowledgeJSON(measurementEntry, entry, changeList, confidenceResult, tRelation, ((double) diff) / 100, viewName, tValue);

               LOG.info("Version: {} vs {} Klasse: {}#{}", entry.getKey(), entry.getValue().getPreviousVersion(), measurementEntry.getTestClass(),
                     measurementEntry.getTestMethod());
               LOG.debug("Confidence Interval Comparison: {}", confidenceResult);

               // System.out.println(builderPrev.getDataSnapshot().getRunCount() + " " + builderCurrent.getDataSnapshot().getRunCount() + " " + prevResults.size() + " "
               // + currentResults.size());

               System.out.println("git diff " + entry.getKey() + ".." + VersionComparator.getPreviousVersion(entry.getKey()));
               System.out.println("vim " + viewName);
            }
         }
      }
      versions.addAll(measurementEntry.getMeasurements().keySet());
      LOG.debug("Version: {}", measurementEntry.getMeasurements().keySet());
      testcases += measurementEntry.getMeasurements().size();
   }

   private File generatePlots(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, boolean change) {
      final List<Result> currentValues = MinimalExecutionDeterminer.cutValuesMiddle(entry.getValue().getCurrent());
      final List<Result> previousValues = MinimalExecutionDeterminer.cutValuesMiddle(entry.getValue().getPrevius());

      final MeanCoVData data = new MeanCoVData(measurementEntry.getTestMethod(), currentValues);
      final MeanCoVData dataPrev = new MeanCoVData(measurementEntry.getTestMethod(), previousValues);

      final MeanHistData histData = new MeanHistData(currentValues);
      final MeanHistData histDataPrev = new MeanHistData(previousValues);

      final File folder = new File(AnalyseOneTest.RESULTFOLDER, "graphs/" + entry.getKey() + "/" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod());
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
         histData.printHistData(new File(folder, "current_hist.csv"));
         histDataPrev.printHistData(new File(folder, "prev_hist.csv"));

         data.printAverages(new File(folder, "current.csv"));
         dataPrev.printAverages(new File(folder, "prev.csv"));

         final int binwidth = (int) (histData.getSpan() / 10);

         final String init = "set datafile separator ';';set decimalsign locale; set decimalsign \",\";set term png;";

         executeGnuplot(folder, "gnuplot", "-e",
               init + "set output 'graph.png'; plot 'current.csv' u ($0*" + data.getAvgCount() + "):1, 'prev.csv' u ($0*" + data.getAvgCount() + "):1");
         executeGnuplot(folder, "gnuplot", "-e", init
               + "set output 'histogram.png'; binwidth=" + binwidth
               + "; bin(x,width)=width*floor(x/width); plot 'current_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes, 'prev_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes");
         executeGnuplot(folder, "gnuplot", "-e", init
               + "set output 'histogram_first.png'; binwidth=" + binwidth
               + "; bin(x,width)=width*floor(x/width); plot 'current_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes");
         executeGnuplot(folder, "gnuplot", "-e", init
               + "set output 'histogram_second.png'; binwidth=" + ((int) (histDataPrev.getSpan() / 10))
               + "; bin(x,width)=width*floor(x/width); plot 'prev_hist.csv' using (bin($1,binwidth)):(1.0) smooth freq with boxes");

         final String filename = entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod() + ".png";
         final String name = entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod() + "_1.png";
         final String name2 = entry.getKey().substring(0, 6) + "_" + measurementEntry.getTestClass() + "." + measurementEntry.getTestMethod() + "_2.png";
         if (MultimodalUtil.isMultimodalCoefficient(entry.getValue().getCurrent()) || MultimodalUtil.isMultimodalCoefficient(entry.getValue().getPrevius())) {
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

         return new File(folder, "graph.png");
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   private void executeGnuplot(final File folder, String... command) throws IOException {
      final ProcessBuilder pb2 = new ProcessBuilder(command);
      pb2.directory(folder);
      final Process p2 = pb2.start();
      de.peran.utils.StreamGobbler.showFullProcess(p2);
   }

   private static void removeOutliers(List<Result> previus) {
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

   private void updateKnowledgeJSON(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final Changes changeList, final Relation confidenceResult,
         final Relation anovaResult, final double anovaDeviation, final String viewName, final double tvalue) {
      try {

         final Change currentChange = changeList.addChange(measurementEntry.getTestClass(), viewName, measurementEntry.getTestMethod(), anovaDeviation, tvalue);

         final Changes version = oldKnowledge.getVersion(entry.getKey());
         if (version != null) {
            final List<Change> oldChanges = version.getTestcaseChanges().get(measurementEntry.getTestClass());
            if (oldChanges != null) {
               for (final Change oldChange : oldChanges) {
                  if (oldChange.getDiff().equals(viewName)) {
                     currentChange.setCorrectness(oldChange.getCorrectness());
                     currentChange.setType(oldChange.getType());
                  }
               }
            }
         }

         knowledge.setVersions(AnalyseFullData.versions.size());
         knowledge.setTestcases(AnalyseFullData.testcases);
         knowledge.setChanges(AnalyseFullData.changes);

         final ObjectMapper objectMapper = new ObjectMapper();
         objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
         objectMapper.writeValue(changeKnowledgeFile, knowledge);

         csvResultWriter.write(entry.getKey() + ";" + "vim " + viewName + ";" + measurementEntry.getTestClass() + ";" + anovaResult + ";" + confidenceResult + "\n");
         csvResultWriter.flush();
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}
