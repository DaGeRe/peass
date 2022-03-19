package de.dagere.peass.reexecutions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.analysis.helper.read.TestcaseData;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.measurement.utils.CreateScriptStarter;

public class MissingExecutionFinder {

   private static final Logger LOG = LogManager.getLogger(MissingExecutionFinder.class);

   private final String project;
   private final File reexecuteFolder;
   private final ExecutionData tests;
   private final String experimentId;

   public MissingExecutionFinder(final String project, final File reexecuteFolder, final ExecutionData tests, final String experimentId) {
      this.project = project;
      this.reexecuteFolder = reexecuteFolder;
      this.tests = tests;
      this.experimentId = experimentId;
   }

   public void findMissing(final File[] dataFolders)
         throws JAXBException, FileNotFoundException, IOException {

      int countAll = tests.getAllExecutions();

      for (File dataFolder : dataFolders) {
         if (dataFolder.exists()) {
            System.out.println(dataFolder.getAbsolutePath());
            removeFinishedExecutions(dataFolder);
         } else {
            LOG.error("Datafolder {} does not exist - adding ALL executions", dataFolder);
         }
      }
      
      System.out.println();
      System.out.println("Missing Tests");

      int countNotExecuted = tests.getAllExecutions();
      System.out.println("Not executed: " + countNotExecuted + " All defined executions: " + countAll);

      final PrintStream outputStream = new PrintStream(new FileOutputStream(new File(reexecuteFolder, "reexecute-missing-" + project + ".sh")));
      CreateScriptStarter.generateExecuteCommands(tests, experimentId, outputStream);
      outputStream.flush();
      outputStream.close();

   }

   public void removeFinishedExecutions(final File folder) throws JAXBException {
      removeSlurmExecutions(folder);
      removeXMLExecutions(folder);
   }

   private void removeXMLExecutions(final File folder) throws JAXBException {
      for (final File measurementFile : folder.listFiles()) {
         if (measurementFile.getName().endsWith(".xml")) {
            LOG.info("File:" + measurementFile);
            final Kopemedata data = new XMLDataLoader(measurementFile).getFullData();
            for (final TestcaseType testcase : data.getTestcases().getTestcase()) {
               final String testmethod = testcase.getName();
               for (final Chunk c : testcase.getDatacollector().get(0).getChunk()) {
                  final String version = findVersion(c);
                  LOG.debug("Removing {}", version);
                  final TestSet versionsTests = tests.getVersions().get(version);
                  if (versionsTests != null) {
                     removeTestFromTestSet(data.getTestcases().getClazz(), testmethod, versionsTests);
                  }
               }
            }
         }
      }
   }

   private void removeSlurmExecutions(final File folder) throws JAXBException {
      ChangeReader reader = new ChangeReader(folder.getName(), tests);
      reader.readFile(folder);
      for (Entry<String, TestcaseData> entry : reader.getAllData().getData().entrySet()) {
         String version = entry.getKey();
         TestSet versionsTests = tests.getVersions().get(version);
         LOG.debug("Removing from: {}", version);
         for (TestCase test : entry.getValue().getTestcaseData().keySet()) {
            removeTestFromTestSet(test.getClazz(), test.getMethod(), versionsTests);
         }
      }
   }

   public static void removeTestFromTestSet(final String clazz, final String testmethod, final TestSet versionsTests) {
      LOG.debug(versionsTests.classCount());
      final TestCase ce = new TestCase(clazz, "");
      versionsTests.removeTest(ce, testmethod);
      LOG.debug(versionsTests.classCount());
   }

   public static String findVersion(final Chunk c) {
      final int size = c.getResult().size();
      final Result r = c.getResult().get(size - 1);
      final String version = r.getVersion().getGitversion();
      LOG.trace("Version: " + version);
      return version;
   }

}
