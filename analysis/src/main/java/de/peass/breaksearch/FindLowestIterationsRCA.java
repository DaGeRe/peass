package de.peass.breaksearch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.dagere.peass.measurement.rca.KiekerResultReader;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CallTreeStatistics;
import de.dagere.peass.measurement.rca.data.OneVMResult;
import de.dagere.peass.utils.Constants;
import kieker.analysis.exception.AnalysisConfigurationException;

class ValueVMResult implements OneVMResult {
   List<Long> values = new LinkedList<Long>();

   @Override
   public double getAverage() {
      final double average = values.stream().mapToLong(l -> l).average().getAsDouble();
      return average;
   }

   public double getMedian(final int iterations) {
      final DescriptiveStatistics stat = getStatistic(iterations);
      return stat.getPercentile(0.5);
   }

   public double getMin(final int iterations) {
      final DescriptiveStatistics stat = getStatistic(iterations);
      return stat.getMin();
   }

   private DescriptiveStatistics getStatistic(final int iterations) {
      final DescriptiveStatistics stat = new DescriptiveStatistics();
      values.subList(0, iterations).forEach(value -> stat.addValue(value));
      return stat;
   }

   public void add(final Long duration) {
      values.add(duration);
   }

   public List<Long> subList(final int i, final int iterations) {
      return values.subList(i, iterations);
   }

   @Override
   public long getCalls() {
      throw new RuntimeException("Not implemented yet.");
   }

   @Override
   public List<StatisticalSummary> getValues() {
      throw new RuntimeException("Not implemented yet.");
   }
}

/**
 * Call tree statistic recording all data
 *
 */
class FullCallTreeStatistic extends CallTreeStatistics {

   private final List<ValueVMResult> results = new ArrayList<>();

   public FullCallTreeStatistic(final int warmup) {
      super(warmup);
   }

   @Override
   public void addMeasurement(final Long duration) {
      results.get(results.size() - 1).add(duration);
   }

   @Override
   public void newResult() {
      results.add(new ValueVMResult());
   }

   @Override
   public SummaryStatistics getStatistics() {
      final SummaryStatistics statistics = new SummaryStatistics();
      for (final ValueVMResult vals : results) {
         statistics.addValue(vals.getAverage());
      }
      return statistics;
   }

   public SummaryStatistics getMedianStatistics(final int iterations) {
      final SummaryStatistics statistics = new SummaryStatistics();
      for (final ValueVMResult vals : results) {
         final double average = vals.getMedian(iterations);
         statistics.addValue(average);
      }
      return statistics;
   }

   public SummaryStatistics getMinStatistics(final int iterations) {
      final SummaryStatistics statistics = new SummaryStatistics();
      for (final ValueVMResult vals : results) {
         final double min = vals.getMin(iterations);
         statistics.addValue(min);
      }
      return statistics;
   }

   public SummaryStatistics getStatistics(final int iterations) {
      final SummaryStatistics statistics = new SummaryStatistics();
      for (final ValueVMResult vals : results) {
         final List<Long> shortenedList = vals.subList(0, iterations);
         final double average = shortenedList.stream().mapToLong(l -> l).average().getAsDouble();
         statistics.addValue(average);
      }
      return statistics;
   }

   public boolean hasValues() {
      return results.get(0).values.size() > 0;
   }
}

class FullDataCallTreeNode extends CallTreeNode {

   protected FullDataCallTreeNode(final String call, final String kiekerPattern, final String otherKiekerPattern, final CallTreeNode parent) {
      super(call, kiekerPattern, otherKiekerPattern, parent);
   }

   public FullDataCallTreeNode(final CallTreeNode mirrorNode) {
      super(mirrorNode.getCall(), mirrorNode.getKiekerPattern(), mirrorNode.getOtherKiekerPattern(), (MeasurementConfiguration) null);
      for (final CallTreeNode child : mirrorNode.getChildren()) {
         appendChild(child);
      }
   }

   public FullDataCallTreeNode(final CallTreeNode mirrorNode, final FullDataCallTreeNode fullDataCallTreeNode) {
      super(mirrorNode.getCall(), mirrorNode.getKiekerPattern(), mirrorNode.getOtherKiekerPattern(), fullDataCallTreeNode.getParent());
      for (final CallTreeNode child : mirrorNode.getChildren()) {
         appendChild(child);
      }
   }

   public FullDataCallTreeNode appendChild(final CallTreeNode mirrorNode) {
      final FullDataCallTreeNode added = new FullDataCallTreeNode(mirrorNode, this);
      children.add(added);
      return added;
   }

   public TestcaseStatistic getMinTestcaseStatistic(final int iterations) {
      final SummaryStatistics current = ((FullCallTreeStatistic) data.get(config.getVersion())).getMinStatistics(iterations);
      final SummaryStatistics previous = ((FullCallTreeStatistic) data.get(config.getVersionOld())).getMinStatistics(iterations);
      return new TestcaseStatistic(previous, current, data.get(config.getVersionOld()).getCalls(), data.get(config.getVersion()).getCalls());
   }

   public TestcaseStatistic getMedianTestcaseStatistic(final int iterations) {
      final SummaryStatistics current = ((FullCallTreeStatistic) data.get(config.getVersion())).getMedianStatistics(iterations);
      final SummaryStatistics previous = ((FullCallTreeStatistic) data.get(config.getVersionOld())).getMedianStatistics(iterations);
      return new TestcaseStatistic(previous, current, data.get(config.getVersionOld()).getCalls(), data.get(config.getVersion()).getCalls());
   }

   public TestcaseStatistic getTestcaseStatistic(final int iterations) {
      final SummaryStatistics current = ((FullCallTreeStatistic) data.get(config.getVersion())).getStatistics(iterations);
      final SummaryStatistics previous = ((FullCallTreeStatistic) data.get(config.getVersionOld())).getStatistics(iterations);
      return new TestcaseStatistic(previous, current, data.get(config.getVersionOld()).getCalls(), data.get(config.getVersion()).getCalls());
   }

   public boolean hasData() {
      if (data.size() > 0) {
         for (final CallTreeStatistics stat : data.values()) {
            final FullCallTreeStatistic stat2 = (FullCallTreeStatistic) stat;
            if (!stat2.hasValues()) {
               return false;
            }
         }
         return true;
      } else {
         return false;
      }
   }

}

public class FindLowestIterationsRCA {

   final FullDataCallTreeNode rootPredecessor;
   final String mainVersion, predecessor;
   final TestCase testcase;
   final File testmethodFolder;

   private Set<CallTreeNode> nodesPredecessor;

   public FindLowestIterationsRCA(final File folder) throws JsonParseException, JsonMappingException, IOException {
      final CauseSearchFolders folders = new CauseSearchFolders(folder);
      final File mainVersionFolder = folders.getArchivedFolder().listFiles()[0];
      final File testclassFolder = mainVersionFolder.listFiles()[0];
      testmethodFolder = testclassFolder.listFiles()[0];

      testcase = new TestCase(testclassFolder.getName(), testmethodFolder.getName());
      mainVersion = mainVersionFolder.getName();
      final File treeCache = folders.getTreeCacheFolder(mainVersion, testcase);
      final File potentialCacheFileOld = new File(treeCache, mainVersion);
      predecessor = mainVersion + "~1";
      rootPredecessor = new FullDataCallTreeNode(Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class));
   }

   private void goToNextLevel() {
      final Set<CallTreeNode> children = new HashSet<>();
      for (final CallTreeNode parent : nodesPredecessor) {
         children.addAll(parent.getChildren());
      }
      nodesPredecessor = children;
   }

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, AnalysisConfigurationException {
      final File folder = new File(args[0]);

      new FindLowestIterationsRCA(folder).analyzeRun();
   }

   private void analyzeRun() throws IOException, JsonParseException, JsonMappingException, AnalysisConfigurationException {
      for (final File versionFolder : testmethodFolder.listFiles()) {
         nodesPredecessor = new HashSet<>();
         nodesPredecessor.add(rootPredecessor);
         final String version = versionFolder.getName();
         final File[] levels = versionFolder.listFiles();
         Arrays.sort(levels);
         for (final File levelFolder : levels) {
            readLevel(versionFolder, version, levelFolder);

            goToNextLevel();
         }
      }

      for (int i = 15000; i >= 1000; i -= 1000) {
         printData(rootPredecessor, i);
      }

   }

   private void printData(final FullDataCallTreeNode parent, final int iterations) {
      System.out.println("Node: " + parent.getCall() + " " + iterations);
      if (parent.hasData()) {
         System.out.println("T=" + parent.getTestcaseStatistic(iterations).getTvalue());
         System.out.println("T=" + parent.getMinTestcaseStatistic(iterations).getTvalue());
         System.out.println("T=" + parent.getMedianTestcaseStatistic(iterations).getTvalue());
         for (final CallTreeNode node : parent.getChildren()) {
            printData((FullDataCallTreeNode) node, iterations);
         }
      }

   }

   private void readLevel(final File versionFolder, final String version, final File levelFolder) throws AnalysisConfigurationException {
      nodesPredecessor.forEach(node -> node.setVersions(mainVersion, predecessor));
      final KiekerResultReader kiekerResultReader = new KiekerResultReader(false, nodesPredecessor, version, testcase, version.equals(mainVersion));
      System.out.println("Reading: " + versionFolder.getName() + " from " + levelFolder + " Nodes: " + nodesPredecessor);
      for (final File kiekerResultFolder : levelFolder.listFiles((FilenameFilter) new RegexFileFilter("[0-9]*"))) {
         final File kiekerTraceFile = KiekerFolderUtil.getKiekerTraceFolder(kiekerResultFolder, testcase);
         kiekerResultReader.readNonAggregated(kiekerTraceFile);
      }
      nodesPredecessor.forEach(node -> node.createStatistics(version));
   }
}
