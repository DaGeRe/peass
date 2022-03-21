package de.dagere.peass.analysis.changes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.analysis.helper.read.VersionData;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.dataloading.KoPeMeDataHelper;
import de.dagere.peass.measurement.statistics.ConfidenceIntervalInterpretion;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;
import de.dagere.peass.measurement.statistics.data.DescribedChunk;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.measurement.utils.RunCommandWriterRCA;
import de.dagere.peass.measurement.utils.RunCommandWriterSlurmRCA;
import de.dagere.peass.utils.Constants;

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

   public StatisticsConfig config = new StatisticsConfig();

   private double minChange = 0;

   private final VersionData allData = new VersionData();
   // private static VersionKnowledge oldKnowledge;
   private final ResultsFolders resultsFolders;

   private final RunCommandWriterRCA runCommandWriter;
   private final RunCommandWriterSlurmRCA runCommandWriterSlurm;
   private final SelectedTests selectedTests;
   
   private Map<String, TestSet> tests;

   public ChangeReader(final ResultsFolders resultsFolders, final SelectedTests selectedTests) throws FileNotFoundException {
      this.selectedTests = selectedTests;
      this.resultsFolders = resultsFolders;
      File statisticsFolder = resultsFolders.getStatisticsFile().getParentFile();
      if (selectedTests.getUrl() != null && !selectedTests.getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(statisticsFolder, "run-rca-" + resultsFolders.getProjectName() + ".sh"));
         runCommandWriter = new RunCommandWriterRCA(runCommandPrinter, "default", selectedTests);
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticsFolder, "run-rca-slurm-" + resultsFolders.getProjectName() + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(runCommandPrinterRCA, "default", selectedTests);
      } else {
         runCommandWriter = null;
         runCommandWriterSlurm = null;
      }
   }

   public ChangeReader(final ResultsFolders resultsFolders, final RunCommandWriterRCA runCommandWriter, final RunCommandWriterSlurmRCA runCommandWriterSlurm, final SelectedTests selectedTests) throws FileNotFoundException {
      this.resultsFolders = resultsFolders;
      this.runCommandWriter = runCommandWriter;
      this.runCommandWriterSlurm = runCommandWriterSlurm;
      this.selectedTests = selectedTests;

   }

   public void setTests(final Map<String, TestSet> tests) {
      this.tests = tests;
   }

   public ChangeReader(final String projectName, final SelectedTests dependencies) {
      this.resultsFolders = null;
      runCommandWriter = null;
      runCommandWriterSlurm = null;
      this.selectedTests = dependencies;
   }

   public void setConfig(StatisticsConfig config) {
      this.config = config;
   }
   
   public StatisticsConfig getConfig() {
      return config;
   }

   public ProjectChanges readFile(final File measurementFolder) throws JAXBException {
      final ProjectChanges changes = new ProjectChanges(config);
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

   private void readCleanFolder(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info, final File cleanParentFolder) throws JAXBException {
      LOG.info("Handling: {}", cleanParentFolder);
      for (File cleanedFolder : cleanParentFolder.listFiles()) {
         for (File childFile : cleanedFolder.listFiles()) {
            if (childFile.getName().endsWith(".xml")) {
               readFile(measurementFolder, changes, info, childFile);
            }
         }
      }
   }

   private void writeResults(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info) {
      if (resultsFolders != null) {
         final String measurementFolderName = measurementFolder.getName();
         String executorName = measurementFolderName.substring(measurementFolderName.lastIndexOf(File.separator) + 1);
         if (executorName.endsWith("_peass")) {
            executorName = executorName.substring(0, executorName.length() - "_peass".length());
         }
         final File resultfile = resultsFolders.getChangeFile();
         final File statisticFile = resultsFolders.getStatisticsFile();
         try {
            Constants.OBJECTMAPPER.writeValue(resultfile, changes);
            Constants.OBJECTMAPPER.writeValue(statisticFile, info);
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
         final String[] versions = KoPeMeDataHelper.getVersions(chunk, selectedTests);
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
         LOG.error("Too few measurements: {} - {} measurements, {} - {} measurements ", versions[0], describedChunk.getDescPrevious().getN(), versions[1],
               describedChunk.getDescCurrent().getN());
      }
   }

   public void getIsChange(final String fileName, final Kopemedata data, final ProjectChanges changeKnowledge, final ProjectStatistics info,
         final String[] versions, final DescribedChunk describedChunk) {
      LOG.debug(data.getTestcases().getClazz());
      final TestcaseStatistic statistic = describedChunk.getStatistic(config);
      statistic.setPredecessor(versions[0]);
      // if (! (statistic.getTvalue() == Double.NaN)){
      CompareData cd = new CompareData(describedChunk.getPrevious(), describedChunk.getCurrent());
      final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(cd);
      final TestCase testcase = getTestcase(data, versions, describedChunk);

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

   private TestCase getTestcase(final Kopemedata data, final String[] versions, final DescribedChunk describedChunk) {
      TestCase testcase = new TestCase(data.getTestcases());
      if (tests != null) {
         TestSet testsOfThisVersion = tests.get(versions[1]);
         for (TestCase test : testsOfThisVersion.getTests()) {
            if (paramsEqual(testcase.getParams(), test)) {
               if (test.getClazz().equals(testcase.getClazz()) && test.getMethod().equals(testcase.getMethod())) {
                  testcase = test;
               }
            }
         }
      }
      return testcase;
   }

   private boolean paramsEqual(final String paramString, final TestCase test) {
      boolean bothNull = test.getParams() == paramString && test.getParams() == null;
      boolean stringEmptyAndParamsNull = "".equals(paramString) && test.getParams() == null;
      return bothNull 
            || stringEmptyAndParamsNull 
            || (test.getParams() != null && test.getParams().equals(paramString)); // last should only be evaluated if both are not null
   }

   private void writeRunCommands(final String[] versions, final DescribedChunk describedChunk, final TestCase testcase) {
      if (runCommandWriter != null) {
         final Result exampleResult = describedChunk.getCurrent().get(0);
         final int iterations = (int) exampleResult.getIterations();
         final int repetitions = (int) exampleResult.getRepetitions();
         final int vms = describedChunk.getCurrent().size();

         final int versionIndex = Arrays.binarySearch(selectedTests.getVersionNames(), versions[1]);
         
         runCommandWriter.createSingleMethodCommand(versionIndex, versions[1], testcase.getExecutable(),
               (int) exampleResult.getWarmup(), iterations, repetitions, vms);

         runCommandWriterSlurm.createSingleMethodCommand(versionIndex, versions[1], testcase.getExecutable(),
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
