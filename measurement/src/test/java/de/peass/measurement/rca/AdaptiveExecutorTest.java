package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.util.FileUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ GitUtils.class, VersionControlSystem.class })
@PowerMockIgnore("javax.management.*")
public class AdaptiveExecutorTest {

   private static final Logger LOG = LogManager.getLogger(AdaptiveExecutorTest.class);

   private static final File SOURCE_DIR = new File("src/test/resources/rootCauseIT/basic_state/");
   private final TestCase TEST = new TestCase("defaultpackage.TestMe", "testMe");

   private File tempDir = TestConstants.getCurrentFolder();
   private File projectFolder;
   private CauseTester executor;

   @Before
   public void setUp() {
      try {
         projectFolder = new File(tempDir, "project");

         FileUtil.copyDir(SOURCE_DIR, projectFolder);

         VCSTestUtils.mockGetVCS();

         PowerMockito.mockStatic(GitUtils.class);

         VCSTestUtils.mockGoToTagAny(SOURCE_DIR);
         final MeasurementConfiguration config  = new MeasurementConfiguration(2, "000001", "000001~1");
         config.setUseKieker(true);
         config.setIterations(2);
         config.setRepetitions(2);
         executor = new CauseTester(new CauseSearchFolders(projectFolder), config, TestConstants.SIMPLE_CAUSE_CONFIG_TESTME);
         LOG.debug("Executor: {}", executor);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testOneMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final Set<CallTreeNode> included = new HashSet<>();
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#child1", 
            "public void defaultpackage.NormalDependency.child1()", "public void defaultpackage.NormalDependency.child1()", new MeasurementConfiguration(5));
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.setVersions("000001", "000001~1"));

      executor.evaluate(TEST);

      executor.getDurations(0);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001~1").getN());
      Assert.assertEquals(8, nodeWithDuration.getCallCount("000001"));
      Assert.assertEquals(8, nodeWithDuration.getCallCount("000001~1"));
   }

   @Test
   public void testConstructorExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final Set<CallTreeNode> included = new HashSet<>();
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#<init>", 
            "public new defaultpackage.NormalDependency.<init>()", "public new defaultpackage.NormalDependency.<init>()", new MeasurementConfiguration(5));
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.setVersions("000001", "000001~1"));

      executor.evaluate(TEST);

      executor.getDurations(1);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001~1").getN());
   }

   public void testMultipleMethodExecution() {
      // TODO Auto-generated method stub
   }
}
