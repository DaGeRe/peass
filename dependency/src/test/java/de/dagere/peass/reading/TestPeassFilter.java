package de.dagere.peass.reading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.folders.PeassFolders;

public class TestPeassFilter {
   
   private static final Logger LOG = LogManager.getLogger(TestPeassFilter.class);
   
   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   private static final File CURRENT = new File(new File("target"), "current");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "state_with_parameters");

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());
      cleanup();
   }

   private void cleanup() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.deleteDirectory(TestConstants.CURRENT_PEASS);
      FileUtils.copyDirectory(BASIC_STATE, CURRENT);
      LOG.info("Created test folder {} Exists: {}", CURRENT.getAbsolutePath(), CURRENT.exists());
   }

   @Test
   public void testExecution() throws ViewNotFoundException, IOException, XmlPullParserException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      Assume.assumeFalse(EnvironmentVariables.isWindows());
      PeassFolders folders = new PeassFolders(CURRENT);
      final KiekerResultManager manager = new KiekerResultManager(folders, new ExecutionConfig(5), new KiekerConfig(true), new EnvironmentVariables());
      final TestSet testset = new TestSet();
      final TestCase testcase = new TestCase("defaultpackage.TestMe", "testMe", "");
      testset.addTest(testcase);
      
      final ModuleClassMapping mapping = new ModuleClassMapping(manager.getExecutor());
      final List<TraceElement> referenceTrace = regenerateTrace(manager, testset, testcase, mapping, 0);

      for (int i = 1; i <= 3; i++) {
         final List<TraceElement> compareTrace = regenerateTrace(manager, testset, testcase, mapping, i);
         
         checkRegeneratedTrace(referenceTrace, compareTrace);
      }

   }

   private void checkRegeneratedTrace(final List<TraceElement> referenceTrace, final List<TraceElement> compareTrace) {
      LOG.debug("Old");
      for (final TraceElement reference : referenceTrace){
         LOG.debug(reference.getClazz() + " " + reference.getMethod());
      }
      
      LOG.debug("New");
      
      for (final TraceElement reference : compareTrace){
         LOG.debug(reference.getClazz() + " " + reference.getMethod());
      }
      
      Assert.assertEquals(referenceTrace.size(), compareTrace.size());
      System.out.println(compareTrace.get(3).toString());
      Assert.assertEquals("defaultpackage.NormalDependency#innerMethod(java.lang.Integer)", compareTrace.get(3).toString());
   }

   private List<TraceElement> regenerateTrace(final KiekerResultManager manager, final TestSet testset, final TestCase testcase, final ModuleClassMapping mapping, final int i)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, FileNotFoundException {
      cleanup();
      PeassFolders peassFolders = new PeassFolders(CURRENT);
      manager.getExecutor().loadClasses();
      manager.executeKoPeMeKiekerRun(testset, ""+i, peassFolders.getDependencyLogFolder());
      final File kiekerFolderComparison = KiekerFolderUtil.getClazzMethodFolder(testcase, manager.getXMLFileFolder(CURRENT))[0];
      LOG.debug("Searching: " + kiekerFolderComparison);
      final List<TraceElement> compareTrace = new CalledMethodLoader(kiekerFolderComparison, mapping).getShortTrace("");
      return compareTrace;
   }
}
