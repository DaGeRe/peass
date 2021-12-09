package de.dagere.peass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.helper.read.FolderValues;
import de.dagere.peass.analysis.helper.read.TestcaseData;
import de.dagere.peass.analysis.helper.read.VersionData;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.measurement.utils.RunCommandWriter;
import de.dagere.peass.measurement.utils.RunCommandWriterSlurm;
import de.dagere.peass.utils.Constants;
import de.peran.analysis.helper.AnalysisUtil;
import de.peran.analysis.helper.comparedata.BigDiffs;

public class GetChangesBetweensameMeasurements {
   
   private PrintStream reexecuteWriter;
   private final VersionData allData;
   
   public GetChangesBetweensameMeasurements(final VersionData allData) {
      this.allData = allData;
      try {
         File destination = new File(AnalysisUtil.getProjectResultFolder(), "reexecute.sh");
         reexecuteWriter = new PrintStream(new FileOutputStream(destination));
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public void examineDiffs() throws IOException, JsonGenerationException, JsonMappingException {
      int changes = 0;
      int instances = 0;
      final BigDiffs diffs = new BigDiffs();
      // Map<String, >
      RunCommandWriter writer = new RunCommandWriterSlurm(reexecuteWriter, "reexecute", VersionComparator.getDependencies());
      for (final Entry<String, TestcaseData> versionEntry : allData.getData().entrySet()) {
         System.out.println(versionEntry.getKey());
         for (final Entry<TestCase, FolderValues> testcaseentry : versionEntry.getValue().getTestcaseData().entrySet()) {
            instances++;
            boolean hadChange = isChange(diffs, versionEntry, testcaseentry);
            if (hadChange) {
               List<String> versions = Arrays.asList(VersionComparator.getDependencies().getVersionNames());
               int versionIndex = versions.indexOf(versionEntry.getKey());
               writer.createSingleMethodCommand(versionIndex, versionEntry.getKey(), testcaseentry.getKey().getExecutable());
               reexecuteWriter.flush();
               changes++;
            }
         }
         Constants.OBJECTMAPPER.writeValue(new File(AnalysisUtil.getProjectResultFolder(), "differingmeasurements.json"), diffs);
      }
      System.out.println("Changes: " + changes + " / " + instances);
   }

   public boolean isChange(final BigDiffs diffs, final Entry<String, TestcaseData> versionEntry, final Entry<TestCase, FolderValues> testcaseentry) {
      System.out.println(" " + testcaseentry.getKey());
      for (final Entry<String, TestcaseStatistic> values : testcaseentry.getValue().getValues().entrySet()) {
         System.out.println("  " + values.getKey() + " " + values.getValue());
      }
      examineMultiMeasurementChange(diffs, versionEntry, testcaseentry);
      boolean anyMeasurementIsChange = false;
      for (final String folder : testcaseentry.getValue().getIsTChange().keySet()) {
         final boolean tChange = testcaseentry.getValue().getIsTChange().get(folder);
         final boolean confidenceChange = testcaseentry.getValue().getIsConfidenceChange().get(folder);
         if (tChange) {
            diffs.incrementTTestDiff(folder);
         }
         if (confidenceChange) {
            diffs.incrementConfidenceDiff(folder);
         }
         if (tChange || confidenceChange) {
            anyMeasurementIsChange = true;
         }
      }
      return anyMeasurementIsChange;
   }

   public void examineMultiMeasurementChange(final BigDiffs diffs, final Entry<String, TestcaseData> versionEntry, final Entry<TestCase, FolderValues> testcaseentry) {
      final Entry<String, TestcaseStatistic> referenceEntry = testcaseentry.getValue().getValues().entrySet().iterator().next();
      final TestcaseStatistic reference = referenceEntry.getValue();
      boolean hadDiff = false;
      for (final Entry<String, TestcaseStatistic> values : testcaseentry.getValue().getValues().entrySet()) {
         diffs.incrementMeasurements(values.getKey());
         if (Math.abs(values.getValue().getTvalue() - reference.getTvalue()) > 4.0) {
            hadDiff = true;
            diffs.incrementDiff(values.getKey());
         }
      }
      if (hadDiff) {
         for (final Entry<String, TestcaseStatistic> values : testcaseentry.getValue().getValues().entrySet()) {
            diffs.addDifferingValue(versionEntry.getKey(), testcaseentry.getKey(), values.getKey(), values.getValue());
         }
      }
   }
}
