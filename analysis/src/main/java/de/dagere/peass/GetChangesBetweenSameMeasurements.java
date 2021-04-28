package de.dagere.peass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.analysis.helper.read.FolderValues;
import de.dagere.peass.analysis.helper.read.TestcaseData;
import de.dagere.peass.analysis.helper.read.VersionData;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.analysis.Cleaner;
import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.dagere.peass.utils.OptionConstants;
import de.dagere.peass.utils.RunCommandWriter;
import de.dagere.peass.utils.RunCommandWriterSlurm;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.AnalysisUtil;
import de.peran.analysis.helper.comparedata.BigDiffs;

/**
 * Compares measurements of same versions and testcases in order to find out whether there results differ.
 * @author reichelt
 *
 */
public class GetChangesBetweenSameMeasurements {

   private PrintStream reexecuteWriter;
   private final VersionData allData;

   public static void main(final String[] args) throws JAXBException, JsonGenerationException, JsonMappingException, IOException, ParseException {
      final long start = System.currentTimeMillis();
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
      options.addOption(FolderSearcher.DATAOPTION);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      Cleaner.loadDependencies(line);
      final String projectName = VersionComparator.getProjectName();
      AnalysisUtil.setProjectName(projectName);
//      oldKnowledge = VersionKnowledge.getOldChanges();
      
      
      for (int i = 0; i < line.getOptionValues(FolderSearcher.DATA).length; i++) {
         final String fileName = line.getOptionValues(FolderSearcher.DATA)[i];
         final ChangeReader reader = new ChangeReader(new File("results"), projectName);
         final File measurementFolder = new File(fileName);
         reader.readFile(measurementFolder);
         final GetChangesBetweenSameMeasurements comparator = new GetChangesBetweenSameMeasurements(reader.getAllData());
         comparator.examineDiffs();
      }
      

      
      final long end = System.currentTimeMillis();
      System.out.println("Duration: "+ (end - start / 10E6));
   }

   public GetChangesBetweenSameMeasurements(final VersionData allData) {
      this.allData = allData;
      try {
         File destination = new File(AnalysisUtil.getProjectResultFolder(), "reexecute.sh");
         reexecuteWriter = new PrintStream(new FileOutputStream(destination));
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void examineDiffs() throws IOException, JsonGenerationException, JsonMappingException {
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
         FolderSearcher.MAPPER.writeValue(new File(AnalysisUtil.getProjectResultFolder(), "differingmeasurements.json"), diffs);
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
