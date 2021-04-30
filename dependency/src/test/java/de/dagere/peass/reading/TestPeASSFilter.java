package de.dagere.peass.reading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;

public class TestPeASSFilter {
   
   private static final Logger LOG = LogManager.getLogger(TestPeASSFilter.class);
   
   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   private static final File CURRENT = new File(new File("target"), "current");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());
      cleanup();
   }

   private void cleanup() throws IOException {
      FileUtils.deleteDirectory(CURRENT);
      FileUtils.deleteDirectory(new File(CURRENT.getParentFile(), CURRENT.getName() + "_peass"));
      FileUtils.copyDirectory(BASIC_STATE, CURRENT);
      LOG.info("Created test folder {} Exists: {}", CURRENT.getAbsolutePath(), CURRENT.exists());
   }

   @Test
   public void testExecution() throws ViewNotFoundException, IOException, XmlPullParserException, InterruptedException {
      final KiekerResultManager manager = new KiekerResultManager(new PeASSFolders(CURRENT), new ExecutionConfig(5), new EnvironmentVariables());
      final TestSet ts = new TestSet();
      final TestCase testcase = new TestCase("defaultpackage.TestMe", "testMe", "");
      ts.addTest(testcase);
      manager.getExecutor().loadClasses();
      manager.executeKoPeMeKiekerRun(ts, "0");
      
      final File kiekerFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, manager.getXMLFileFolder(CURRENT));
      LOG.debug("Searching: " + kiekerFolder);
      final ModuleClassMapping mapping = new ModuleClassMapping(manager.getExecutor());
      final List<TraceElement> referenceTrace = new CalledMethodLoader(kiekerFolder, mapping).getShortTrace("");

      for (int i = 1; i <= 3; i++) {
         final List<TraceElement> compareTrace = regenerateTrace(manager, ts, testcase, mapping, i);
         
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

   private List<TraceElement> regenerateTrace(final KiekerResultManager manager, final TestSet ts, final TestCase testcase, final ModuleClassMapping mapping, final int i)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, FileNotFoundException {
      cleanup();
      new PeASSFolders(CURRENT);
      manager.getExecutor().loadClasses();
      manager.executeKoPeMeKiekerRun(ts, ""+i);
      final File kiekerFolderComparison = KiekerFolderUtil.getClazzMethodFolder(testcase, manager.getXMLFileFolder(CURRENT));
      LOG.debug("Searching: " + kiekerFolderComparison);
      final List<TraceElement> compareTrace = new CalledMethodLoader(kiekerFolderComparison, mapping).getShortTrace("");
      return compareTrace;
   }
}
