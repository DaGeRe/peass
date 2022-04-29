package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
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

public class AdaptiveExecutorMoreParameterIT {

   private static final Logger LOG = LogManager.getLogger(AdaptiveExecutorMoreParameterIT.class);

   private static final File SOURCE_DIR = new File("src/test/resources/rootCauseIT/basic_state_moreparameters/");
   private static final TestCase TEST = new TestCase("defaultpackage.TestMe", "testMe");
   public static CauseSearcherConfig FULL_CASE_CONFIG = new CauseSearcherConfig(TEST, false, 0.1, false, false, RCAStrategy.COMPLETE, 1);

   private final File projectFolder = TestConstants.CURRENT_FOLDER;
   private CauseTester executor;

   @Rule
   public OnFailureLogSafer logSafer = new OnFailureLogSafer(TestConstants.CURRENT_FOLDER,
         new File(TestConstants.CURRENT_FOLDER.getParentFile(), TestConstants.CURRENT_FOLDER.getName() + "_peass"));

   @Before
   public void setUp() {
      try {
         TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
         TestUtil.deleteContents(TestConstants.CURRENT_PEASS);

         FileUtil.copyDir(SOURCE_DIR, projectFolder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void testSuccessfull()
         throws IOException, InterruptedException, FileNotFoundException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      LOG.debug("Executor: {}", executor);
      final Set<CallTreeNode> included = new HashSet<>();
      final String kiekerPattern = "public void defaultpackage.NormalDependency.child1(int[],double,java.lang.String)";
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#child1", kiekerPattern, kiekerPattern,
            new MeasurementConfig(5, "000001", "000001~1"));
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.initVersions());

      executor.evaluate(TEST);

      executor.getDurations(0);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001~1").getN());
   }

   @Test
   public void testFullMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<GitUtils> mockedGitUtils = Mockito.mockStatic(GitUtils.class)) {
         VCSTestUtils.mockGetVCS(mockedVCS);
         VCSTestUtils.mockGoToTagAny(mockedGitUtils, SOURCE_DIR);
         CauseSearchFolders folders = new CauseSearchFolders(projectFolder);
         executor = new CauseTester(folders, TestConstants.SIMPLE_MEASUREMENT_CONFIG_KIEKER, FULL_CASE_CONFIG, new EnvironmentVariables());
         testSuccessfull();
      }
   }

   @Test
   public void testOneMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<GitUtils> mockedGitUtils = Mockito.mockStatic(GitUtils.class)) {
         VCSTestUtils.mockGetVCS(mockedVCS);
         VCSTestUtils.mockGoToTagAny(mockedGitUtils, SOURCE_DIR);
         CauseSearchFolders folders = new CauseSearchFolders(projectFolder);
         executor = new CauseTester(folders, TestConstants.SIMPLE_MEASUREMENT_CONFIG_KIEKER, TestConstants.SIMPLE_CAUSE_CONFIG_TESTME, new EnvironmentVariables());
         testSuccessfull();
      }
   }

}
