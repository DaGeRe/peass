package de.peran.analysis.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.utils.Log;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.OptionConstants;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.comparedata.BigDiffs;
import de.peran.analysis.helper.read.FolderValues;
import de.peran.analysis.helper.read.TestcaseData;
import de.peran.analysis.helper.read.VersionData;
import de.peran.measurement.analysis.Statistic;

public class CompareValues {

   private BufferedWriter reexecuteWriter;
   private final VersionData allData;

   public static void main(final String[] args) throws JAXBException, JsonGenerationException, JsonMappingException, IOException, ParseException {
      long start = System.currentTimeMillis();
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE);
      options.addOption(FolderSearcher.DATAOPTION);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      DependencyReaderUtil.loadDependencies(line);
      String projectName = VersionComparator.getProjectName();
      AnalysisUtil.setProjectName(projectName);
//      oldKnowledge = VersionKnowledge.getOldChanges();
      
      
      for (int i = 0; i < line.getOptionValues(FolderSearcher.DATA).length; i++) {
         String fileName = line.getOptionValues(FolderSearcher.DATA)[i];
         DataReader cleaner = new DataReader(new File("results"), projectName);
         File file = new File(fileName);
         cleaner.readFile(file);
         CompareValues comparator = new CompareValues(cleaner.getAllData());
         comparator.examineDiffs();
      }
      

      
      long end = System.currentTimeMillis();
      Log.info("Duration: {}", end - start / 10E6);
   }

   public CompareValues(VersionData allData) {
      this.allData = allData;
      try {
         reexecuteWriter = new BufferedWriter(new FileWriter(new File(AnalysisUtil.getProjectResultFolder(), "reexecute.sh")));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void examineDiffs() throws IOException, JsonGenerationException, JsonMappingException {
      int grossediff = 0;
      int changes = 0;
      final BigDiffs diffs = new BigDiffs();
      // Map<String, >
      for (final Entry<String, TestcaseData> versionEntry : allData.getData().entrySet()) {
         System.out.println(versionEntry.getKey());
         for (final Entry<TestCase, FolderValues> testcaseentry : versionEntry.getValue().getTestcaseData().entrySet()) {
            System.out.println(" " + testcaseentry.getKey());
            for (final Entry<String, Statistic> values : testcaseentry.getValue().getValues().entrySet()) {
               System.out.println("  " + values.getKey() + " " + values.getValue());
            }
            final Entry<String, Statistic> referenceEntry = testcaseentry.getValue().getValues().entrySet().iterator().next();
            final Statistic reference = referenceEntry.getValue();
            boolean hadDiff = false;
            for (final Entry<String, Statistic> values : testcaseentry.getValue().getValues().entrySet()) {
               diffs.incrementMeasurements(values.getKey());
               if (Math.abs(values.getValue().getTvalue() - reference.getTvalue()) > 4.0) {
                  System.out.println("Große Diff!");
                  grossediff++;
                  hadDiff = true;
                  diffs.incrementDiff(values.getKey());
               }
            }
            if (hadDiff) {
               for (final Entry<String, Statistic> values : testcaseentry.getValue().getValues().entrySet()) {
                  diffs.addDifferingValue(versionEntry.getKey(), testcaseentry.getKey(), values.getKey(), values.getValue());
               }
            }
            boolean hadChange = false;
            for (String folder : testcaseentry.getValue().getIsTChange().keySet()) {
               boolean tChange = testcaseentry.getValue().getIsTChange().get(folder);
               boolean confidenceChange = testcaseentry.getValue().getIsConfidenceChange().get(folder);
               if (tChange) {
                  diffs.incrementTTestDiff(folder);
               }
               if (confidenceChange) {
                  diffs.incrementConfidenceDiff(folder);
               }
               if (tChange || confidenceChange) {
                  hadChange = true;
               }
            }
            if (hadChange) {
               reexecuteWriter.write("java -cp target/measurement-0.1-SNAPSHOT.jar de.peran.AdaptiveTestStarter "
                     + "-test " + testcaseentry.getKey().getExecutable() + " "
                     + "-warmup 0 "
                     + "-iterations 1000 "
                     + "-repetitions 100 "
                     + "-vms 1000 "
                     + "-timeout 300 "
                     + "-startversion " + versionEntry.getKey() + " "
                     + "-endversion " + versionEntry.getKey() + " "
                     + "-executionfile ../execute-commons-io.json "
                     + "-folder ../../projekte/commons-io/ "
                     + "-dependencyfile ../dependency/deps_commons-io.xml &> measurement_" + versionEntry.getKey().substring(0, 6) + "_" + testcaseentry.getKey().getExecutable()
                     + ".txt\n");
               reexecuteWriter.flush();
               changes++;
            }
         }
         FolderSearcher.MAPPER.writeValue(new File(AnalysisUtil.getProjectResultFolder(), "differingmeasurements.json"), diffs);
      }
      System.out.println("Große Diff: " + grossediff + " Changes: " + changes);
   }

}
