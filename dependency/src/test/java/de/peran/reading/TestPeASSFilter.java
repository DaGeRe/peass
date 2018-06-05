package de.peran.reading;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.peran.dependency.PeASSFolders;
import de.peran.dependency.TestResultManager;
import de.peran.dependency.analysis.CalledMethodLoader;
import de.peran.dependency.analysis.data.TestCase;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.dependency.analysis.data.TraceElement;
import de.peran.dependencyprocessors.ViewNotFoundException;
import de.peran.dependencytests.ViewGeneratorIT;

public class TestPeASSFilter {
   
   private static final Logger LOG = LogManager.getLogger(TestPeASSFilter.class);
   
   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   private static final File CURRENT = new File(new File("target"), "current");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

   @Before
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());
      cleanup();
   }

   private void cleanup() throws IOException {
      FileUtils.deleteDirectory(CURRENT);
      FileUtils.deleteDirectory(new File(CURRENT.getParentFile(), CURRENT.getName() + "_peass"));
      FileUtils.copyDirectory(BASIC_STATE, CURRENT);
   }

   @Test
   public void testExecution() throws ViewNotFoundException, IOException {
      final TestResultManager manager = new TestResultManager(CURRENT);
      final TestSet ts = new TestSet();
      final TestCase testcase = new TestCase("defaultpackage.TestMe", "testMe");
      ts.addTest(testcase);
      manager.executeKoPeMeKiekerRun(ts, "0");
      
      final File kiekerFolder = ViewGeneratorIT.getMethodFolder(testcase, manager.getXMLFileFolder(CURRENT));
      LOG.debug("Searching: " + kiekerFolder);
      final List<TraceElement> referenceTrace = new CalledMethodLoader(kiekerFolder, CURRENT).getShortTrace("");

      for (int i = 1; i <= 10; i++) {
         cleanup();
         new PeASSFolders(CURRENT);
         manager.executeKoPeMeKiekerRun(ts, ""+i);
         final File kiekerFolderComparison = ViewGeneratorIT.getMethodFolder(testcase, manager.getXMLFileFolder(CURRENT));
         LOG.debug("Searching: " + kiekerFolderComparison);
         final List<TraceElement> compareTrace = new CalledMethodLoader(kiekerFolderComparison, CURRENT).getShortTrace("");
         
         LOG.debug("Old");
         for (final TraceElement reference : referenceTrace){
            LOG.debug(reference.getClazz() + " " + reference.getMethod());
         }
         
         LOG.debug("New");
         
         for (final TraceElement reference : compareTrace){
            LOG.debug(reference.getClazz() + " " + reference.getMethod());
         }
         
         Assert.assertEquals(referenceTrace.size(), compareTrace.size());
      }

   }
}
