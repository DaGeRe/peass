package de.dagere.peass.reexecutions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
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
         throws FileNotFoundException, IOException {

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

   public void removeFinishedExecutions(final File folder) {
      removeSlurmExecutions(folder);
      removeXMLExecutions(folder);
   }

   private void removeXMLExecutions(final File folder) {
      for (final File measurementFile : folder.listFiles()) {
         if (measurementFile.getName().endsWith(".json")) {
            LOG.info("File:" + measurementFile);
            final Kopemedata data = new JSONDataLoader(measurementFile).getFullData();
            for (final TestMethod testcase : data.getMethods()) {
               final String testmethod = testcase.getMethod();
               for (final VMResultChunk c : testcase.getDatacollectorResults().get(0).getChunks()) {
                  final String version = findVersion(c);
                  LOG.debug("Removing {}", version);
                  final TestSet versionsTests = tests.getVersions().get(version);
                  if (versionsTests != null) {
                     removeTestFromTestSet(data.getClazz(), testmethod, versionsTests);
                  }
               }
            }
         }
      }
   }

   private void removeSlurmExecutions(final File folder) {
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

   public static String findVersion(final VMResultChunk c) {
      final int size = c.getResults().size();
      final VMResult r = c.getResults().get(size - 1);
      final String version = r.getCommit();
      LOG.trace("Version: " + version);
      return version;
   }

}
