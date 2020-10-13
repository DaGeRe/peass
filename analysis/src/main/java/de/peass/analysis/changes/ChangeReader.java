package de.peass.analysis.changes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peass.confidence.KoPeMeDataHelper;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.utils.RunCommandWriterSearchCause;
import de.peass.utils.RunCommandWriterSlurmRCA;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.read.VersionData;
import de.peran.measurement.analysis.ProjectStatistics;

/**
 * Reads changes of fulldata - data need to be cleaned!
 * 
 * @author reichelt
 *
 */
public class ChangeReader {

   private static final Logger LOG = LogManager.getLogger(ChangeReader.class);

   private int folderMeasurements = 0;
   private int measurements = 0;
   private int testcases = 0;

   public double type1error = 0.01;
   private double type2error = 0.01;

   private double minChange = 2;

   private final VersionData allData = new VersionData();
   // private static VersionKnowledge oldKnowledge;
   private final File statisticsFolder;

   private final RunCommandWriterSearchCause runCommandWriter;
   private final RunCommandWriterSlurmRCA runCommandWriterSlurm;

   public ChangeReader(final RepoFolders resultsFolder, final String projectName) throws FileNotFoundException {
      statisticsFolder = resultsFolder.getProjectStatisticsFolder(projectName);
      if (VersionComparator.getDependencies().getUrl() != null && !VersionComparator.getDependencies().getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(statisticsFolder, "run-" + projectName + ".sh"));
         runCommandWriter = new RunCommandWriterSearchCause(runCommandPrinter, "default", VersionComparator.getDependencies());
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticsFolder, "run-rca-" + projectName + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(runCommandPrinterRCA, "default", VersionComparator.getDependencies());
      } else {
         runCommandWriter = null;
         runCommandWriterSlurm = null;
      }
   }

   public ChangeReader(final File statisticsFolder, final String projectName) throws FileNotFoundException {
      this.statisticsFolder = statisticsFolder;
      if (VersionComparator.getDependencies().getUrl() != null && !VersionComparator.getDependencies().getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(statisticsFolder, "run-" + projectName + ".sh"));
         runCommandWriter = new RunCommandWriterSearchCause(runCommandPrinter, "default", VersionComparator.getDependencies());
         PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticsFolder, "run-rca-" + projectName + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(runCommandPrinterRCA, "default", VersionComparator.getDependencies());
      } else {
         runCommandWriter = null;
         runCommandWriterSlurm = null;
      }
   }

   public ChangeReader(final String projectName) {
      this.statisticsFolder = null;
      runCommandWriter = null;
      runCommandWriterSlurm = null;
   }

   public double getType1error() {
      return type1error;
   }

   public void setType1error(final double type1error) {
      this.type1error = type1error;
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(final double type2error) {
      this.type2error = type2error;
   }

   public ProjectChanges readFile(final File measurementFolder) throws JAXBException {
      final ProjectChanges changes = new ProjectChanges();
      final ProjectStatistics info = new ProjectStatistics();
      LOG.debug("Reading from " + measurementFolder.getAbsolutePath());
      readFile(measurementFolder, changes, info);

      changes.setTestcaseCount(measurements);
      changes.setVersionCount(info.getStatistics().size());
      writeResults(measurementFolder, changes, info);
      System.out.println("Measurements: " + folderMeasurements + " Tests: " + testcases + " (last read: " + measurementFolder + ")");
      return changes;
   }

   private void readFile(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info) throws JAXBException {
      if (measurementFolder.isDirectory()) {
         for (final File file : measurementFolder.listFiles()) {
            if (file.getName().matches("[0-9]+_[0-9]+")) {
               File slurmCleanFolder = new File(file, "peass/clean");
               readCleanFolder(measurementFolder, changes, info, slurmCleanFolder);
            } else if (file.getName().equals("clean")) {
               readCleanFolder(measurementFolder, changes, info, file);
            } else if (file.getName().endsWith(".xml")) {
               readFile(measurementFolder, changes, info, file);
            }
         }
      } else {
         if (measurementFolder.getName().endsWith(".xml")) {
            readFile(measurementFolder, changes, info, measurementFolder);
         }
      }
   }

   private void readCleanFolder(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info, File cleanFolder) throws JAXBException {
      LOG.info("Handling: {}", cleanFolder);
      File versionFolder = cleanFolder.listFiles()[0].listFiles()[0];
      File testcaseFolder = versionFolder.listFiles()[0].listFiles()[0];
      for (File childFile : testcaseFolder.listFiles()) {
         if (childFile.getName().endsWith(".xml")) {
            readFile(measurementFolder, changes, info, childFile);
         }
      }
   }

   private void writeResults(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info) {
      if (statisticsFolder != null) {
         final String measurementFolderName = measurementFolder.getName();
         String executorName = measurementFolderName.substring(measurementFolderName.lastIndexOf(File.separator) + 1);
         if (executorName.endsWith("_peass")) {
            executorName = executorName.substring(0, executorName.length() - "_peass".length());
         }
         final File resultfile = new File(statisticsFolder.getParentFile(), "changes_" + executorName + ".json");
         final File statisticFile = new File(statisticsFolder, executorName + ".json");
         try {
            FolderSearcher.MAPPER.writeValue(resultfile, changes);
            FolderSearcher.MAPPER.writeValue(statisticFile, info);
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   private void readFile(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info, final File file) throws JAXBException {
      final Kopemedata data = new XMLDataLoader(file).getFullData();
      for (final TestcaseType testcaseMethod : data.getTestcases().getTestcase()) {
         LOG.info(file.getAbsolutePath());
         readTestcase(measurementFolder.getName(), data, testcaseMethod, changes, info);
         measurements += testcaseMethod.getDatacollector().get(0).getChunk().size();
         testcases++;
      }
   }

   private int readTestcase(final String fileName, final Kopemedata data, final TestcaseType testcaseMethod, final ProjectChanges changeKnowledge,
         final ProjectStatistics info) {
      for (final Chunk chunk : testcaseMethod.getDatacollector().get(0).getChunk()) {
         folderMeasurements++;
         final String[] versions = KoPeMeDataHelper.getVersions(chunk);
         LOG.debug(versions[1]);
         if (versions[1] != null) {
            readChunk(fileName, data, changeKnowledge, info, chunk, versions);
         }
      }
      return folderMeasurements;
   }

   private void readChunk(final String fileName, final Kopemedata data, final ProjectChanges changeKnowledge, final ProjectStatistics info, final Chunk chunk,
         final String[] versions) {
      final DescribedChunk describedChunk = new DescribedChunk(chunk, versions[0], versions[1]);
      describedChunk.removeOutliers();

      if (describedChunk.getDescPrevious().getN() > 1 && describedChunk.getDescCurrent().getN() > 1) {
         getIsChange(fileName, data, changeKnowledge, info, versions, describedChunk);
      } else {
         System.out.println("Too few measurements: " + describedChunk.getDescPrevious().getN() + " " + versions[0] + " " + versions[1]);
      }
   }

   public void getIsChange(final String fileName, final Kopemedata data, final ProjectChanges changeKnowledge, final ProjectStatistics info,
         final String[] versions, final DescribedChunk describedChunk) {
      LOG.debug(data.getTestcases().getClazz());
      final TestcaseStatistic statistic = describedChunk.getStatistic(type1error, type2error);
      statistic.setPredecessor(versions[0]);
      // if (! (statistic.getTvalue() == Double.NaN)){
      final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(describedChunk.getPrevious(), describedChunk.getCurrent());
      final TestCase testcase = new TestCase(data);
      final double diff = describedChunk.getDiff();
      final boolean isBigEnoughDiff = Math.abs(diff) > minChange;
      allData.addStatistic(versions[1], testcase, fileName, statistic,
            statistic.isChange() && isBigEnoughDiff,
            !confidenceResult.equals(Relation.EQUAL));
      if (statistic.isChange() && isBigEnoughDiff) {
         changeKnowledge.addChange(testcase, versions[1],
               confidenceResult,
               statistic.isChange() ? Relation.GREATER_THAN : Relation.EQUAL,
               describedChunk.getDescPrevious().getMean(), diff,
               statistic.getTvalue(),
               statistic.getVMs());

         writeRunCommands(versions, describedChunk, testcase);
      }
      info.addMeasurement(versions[1], testcase, statistic);
   }

   private void writeRunCommands(final String[] versions, final DescribedChunk describedChunk, final TestCase testcase) {
      if (runCommandWriter != null) {
         final Result exampleResult = describedChunk.getCurrent().get(0);
         final int iterations = (int) exampleResult.getExecutionTimes();
         final int repetitions = (int) exampleResult.getRepetitions();
         final int vms = describedChunk.getCurrent().size();

         runCommandWriter.createSingleMethodCommand(0, versions[1], testcase.getExecutable(),
               (int) exampleResult.getWarmupExecutions(), iterations, repetitions, vms);
         
         runCommandWriterSlurm.createSingleMethodCommand(0, versions[1], testcase.getExecutable(),
               iterations, repetitions, vms);
      }
   }

   public VersionData getAllData() {
      return allData;
   }

   public double getMinChange() {
      return minChange;
   }

   public void setMinChange(final double minChange) {
      this.minChange = minChange;
   }
}
