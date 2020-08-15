package de.peass.reexecutions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.utils.DivideVersions;

public class MissingExecutionFinder {

   private static final Logger LOG = LogManager.getLogger(MissingExecutionFinder.class);

   private final String project;
   private final File reexecuteFolder;
   private final ExecutionData tests;
   private final String experimentId;

   public MissingExecutionFinder(String project, File reexecuteFolder, ExecutionData tests, String experimentId) {
      this.project = project;
      this.reexecuteFolder = reexecuteFolder;
      this.tests = tests;
      this.experimentId = experimentId;
   }

   public void findMissing(final File dataFolder)
         throws JAXBException, FileNotFoundException, IOException {

      int countAll = tests.getAllExecutions();

      if (dataFolder.exists()) {
         System.out.println(dataFolder.getAbsolutePath());
         removeFinishedExecutions(dataFolder);
      } else {
         LOG.error("Datafolder {} does not exist - adding ALL executions", dataFolder);
      }
      
      System.out.println();
      System.out.println("Missing Tests");

      int countNotExecuted = tests.getAllExecutions();
      System.out.println("Not executed: " + countNotExecuted + " All defined executions: " + countAll);

      final PrintStream outputStream = new PrintStream(new FileOutputStream(new File(reexecuteFolder, "reexecute-missing-" + project + ".sh")));
      DivideVersions.generateExecuteCommands(tests, experimentId, outputStream);
      outputStream.flush();
      outputStream.close();

   }

   public void removeFinishedExecutions(final File folder) throws JAXBException {
      for (final File measurementFile : folder.listFiles()) {
         if (measurementFile.getName().endsWith(".xml")) {
            LOG.info("File:" + measurementFile);
            final Kopemedata data = new XMLDataLoader(measurementFile).getFullData();
            for (final TestcaseType testcase : data.getTestcases().getTestcase()) {
               final String testmethod = testcase.getName();
               for (final Chunk c : testcase.getDatacollector().get(0).getChunk()) {
                  final String version = findVersion(c);
                  LOG.debug("Removing {}", version);
                  final TestSet testSet = tests.getVersions().get(version);
                  if (testSet != null) {
                     removeTestFromTestSet(data, testmethod, testSet);
                  }
               }
            }
         }
      }
   }

   public static void removeTestFromTestSet(final Kopemedata data, final String testmethod, final TestSet testSet) {
      LOG.trace(testSet.classCount());
      final ChangedEntity ce = new ChangedEntity(data.getTestcases().getClazz(), "");
      testSet.removeTest(ce, testmethod);
      LOG.trace(testSet.classCount());
   }

   public static String findVersion(final Chunk c) {
      final int size = c.getResult().size();
      final Result r = c.getResult().get(size - 1);
      final String version = r.getVersion().getGitversion();
      LOG.trace("Version: " + version);
      return version;
   }

}
