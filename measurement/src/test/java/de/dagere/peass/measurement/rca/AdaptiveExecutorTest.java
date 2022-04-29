package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.util.FileUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.OnFailureLogSafer;
import de.dagere.peass.measurement.rca.helper.TestConstants;
import de.dagere.peass.measurement.rca.helper.VCSTestUtils;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;

public class AdaptiveExecutorTest {

   private static final String VERSION0 = "000001~1";

   private static final String VERSION1 = "000001";

   private static final Logger LOG = LogManager.getLogger(AdaptiveExecutorTest.class);

   private static final File SOURCE_DIR = new File("src/test/resources/rootCauseIT/basic_state/");
   private final TestCase TEST = new TestCase("defaultpackage.TestMe", "testMe");

   private File projectFolder = TestConstants.CURRENT_FOLDER;
   private CauseTester executor;
   
   private MeasurementConfig config;

   @Rule
   public OnFailureLogSafer logSafer = new OnFailureLogSafer(TestConstants.CURRENT_FOLDER,
         new File(TestConstants.CURRENT_FOLDER.getParentFile(), TestConstants.CURRENT_FOLDER.getName() + "_peass"));

   @Before
   public void setUp() {
      try {
         TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
         TestUtil.deleteContents(TestConstants.CURRENT_PEASS);

         FileUtil.copyDir(SOURCE_DIR, projectFolder);

         config = new MeasurementConfig(2, VERSION1, VERSION0);
         config.setUseKieker(true);
         config.setIterations(2);
         config.setRepetitions(2);
         executor = new CauseTester(new CauseSearchFolders(projectFolder), config, TestConstants.SIMPLE_CAUSE_CONFIG_TESTME, new EnvironmentVariables());
         LOG.debug("Executor: {}", executor);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testOneMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<GitUtils> mockedGitUtils = Mockito.mockStatic(GitUtils.class)) {
         VCSTestUtils.mockGetVCS(mockedVCS);
         VCSTestUtils.mockGoToTagAny(mockedGitUtils, SOURCE_DIR);

         final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#child1",
               "public void defaultpackage.NormalDependency.child1()", "public void defaultpackage.NormalDependency.child1()", config);
         
         measureNode(nodeWithDuration);

         executor.getDurations(0);

         Assert.assertEquals(2, nodeWithDuration.getStatistics(VERSION1).getN());
         Assert.assertEquals(2, nodeWithDuration.getStatistics(VERSION0).getN());
         Assert.assertEquals(4, nodeWithDuration.getCallCount(VERSION1));
         Assert.assertEquals(4, nodeWithDuration.getCallCount(VERSION0));
      }
   }

   @Test
   public void testConstructorExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<GitUtils> mockedGitUtils = Mockito.mockStatic(GitUtils.class)) {
         VCSTestUtils.mockGetVCS(mockedVCS);
         VCSTestUtils.mockGoToTagAny(mockedGitUtils, SOURCE_DIR);
         
         final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#<init>",
               "public new defaultpackage.NormalDependency.<init>()", "public new defaultpackage.NormalDependency.<init>()", config);
         
         measureNode(nodeWithDuration);

         executor.getDurations(1);

         Assert.assertEquals(2, nodeWithDuration.getStatistics(VERSION1).getN());
         Assert.assertEquals(2, nodeWithDuration.getStatistics(VERSION0).getN());
      }
   }
   
   private void measureNode(final CallTreeNode nodeWithDuration) throws IOException, InterruptedException,  XmlPullParserException {
      final Set<CallTreeNode> included = new HashSet<>();
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.initVersions());

      executor.evaluate(TEST);
   }

   public void testMultipleMethodExecution() {
      // TODO Auto-generated method stub
   }
}
