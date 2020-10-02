package de.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
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
public class AdaptiveExecutorMoreParameterTest {

   private static final Logger LOG = LogManager.getLogger(AdaptiveExecutorMoreParameterTest.class);

   private static final File SOURCE_DIR = new File("src/test/resources/rootCauseIT/basic_state_moreparameters/");
   private static final TestCase TEST = new TestCase("defaultpackage.TestMe", "testMe");
   public static CauseSearcherConfig FULL_CASE_CONFIG = new CauseSearcherConfig(TEST, false, true, 5.0, false, 0.1, false, false);

   private File tempDir;
   private File projectFolder;
   private CauseTester executor;
   private JUnitTestTransformer transformer;

   @Before
   public void setUp() {
      try {
         tempDir = Files.createTempDirectory(new File("target").toPath(), "peass_").toFile();
         projectFolder = new File(tempDir, "project");

         FileUtil.copyDir(SOURCE_DIR, projectFolder);

         VCSTestUtils.mockGetVCS();

         PowerMockito.mockStatic(GitUtils.class);

         VCSTestUtils.mockGoToTagAny(SOURCE_DIR);

         transformer = new JUnitTestTransformer(projectFolder, TestConstants.SIMPLE_MEASUREMENT_CONFIG_KIEKER);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void testSuccessfull()
         throws IOException, InterruptedException, JAXBException, FileNotFoundException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      LOG.debug("Executor: {}", executor);
      final Set<CallTreeNode> included = new HashSet<>();
      final String kiekerPattern = "public void defaultpackage.NormalDependency.child1(int[], double, java.lang.String)";
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#child1", kiekerPattern, kiekerPattern);
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.setVersions("000001", "000001~1"));

      executor.evaluate(TEST);

      executor.getDurations(0);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("000001~1").getN());
   }

   @Test
   public void testFullMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      executor = new CauseTester(new CauseSearchFolders(projectFolder), transformer, FULL_CASE_CONFIG);
      testSuccessfull();
   }

   @Test
   public void testOneMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      executor = new CauseTester(new CauseSearchFolders(projectFolder), transformer, TestConstants.SIMPLE_CAUSE_CONFIG_TESTME);
      testSuccessfull();
   }

}
