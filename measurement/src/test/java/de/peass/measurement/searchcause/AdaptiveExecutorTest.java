package de.peass.measurement.searchcause;

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

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitUtils.class, VersionControlSystem.class})
@PowerMockIgnore("javax.management.*")
public class AdaptiveExecutorTest {
   
   private static final Logger LOG = LogManager.getLogger(AdaptiveExecutorTest.class);

   private static final File SOURCE_DIR = new File("src/test/resources/rootCauseIT/basic_state/");
   private final TestCase TEST = new TestCase("defaultpackage.TestMe", "testMe");
   
   private File tempDir;
   private File projectFolder;
   private CauseTester executor;

   @Before
   public void setUp() {
      try {
         // tempDir = new File("/tmp/peass_1994237287341574028");
         tempDir = Files.createTempDirectory(new File("target").toPath(), "peass_").toFile();
         projectFolder = new File(tempDir, "project");

         FileUtil.copyDir(SOURCE_DIR, projectFolder);
         
         PowerMockito.mockStatic(VersionControlSystem.class);
         VCSTestUtils.mockGetVCS();
         
         PowerMockito.mockStatic(GitUtils.class);
         
         VCSTestUtils.mockGoToTagAny(SOURCE_DIR);
         
         final JUnitTestTransformer transformer = new JUnitTestTransformer(projectFolder);
         transformer.setDatacollectorlist(DataCollectorList.ONLYTIME);
         final MeasurementConfiguration config = new MeasurementConfiguration(2, "00001", "00001~1");
         executor = new CauseTester(new CauseSearchFolders(projectFolder), transformer, config, AdaptiveExecutorMoreParameterTest.DEFAULT_CAUSE_CONFIG);
         LOG.debug("Executor: {}", executor);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testOneMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final Set<CallTreeNode> included = new HashSet<>();
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#child1", "public void defaultpackage.NormalDependency.child1()", null);
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.setVersions("00001", "00001~1"));
      
      executor.evaluate(TEST);

      executor.getDurations(0);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001~1").getN());
   }
   
   @Test
   public void testConstructorExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final Set<CallTreeNode> included = new HashSet<>();
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#<init>", "public new defaultpackage.NormalDependency.<init>()", null);
      nodeWithDuration.setOtherVersionNode(nodeWithDuration);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);
      included.forEach(node -> node.setVersions("00001", "00001~1"));
      
      executor.evaluate(TEST);

      executor.getDurations(1);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001~1").getN());
   }

   public void testMultipleMethodExecution() {
      // TODO Auto-generated method stub
   }
}
