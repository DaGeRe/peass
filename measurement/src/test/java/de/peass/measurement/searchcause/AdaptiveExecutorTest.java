package de.peass.measurement.searchcause;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.util.FileUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;
import de.peass.measurement.MeasurementConfiguration;

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
         mockGetVCS();
         
         PowerMockito.mockStatic(GitUtils.class);
         PowerMockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
               File destFile = (File) invocation.getArgument(1);
               LOG.debug("Loading version..");
               FileUtils.deleteDirectory(destFile);
               FileUtils.copyDirectory(SOURCE_DIR, destFile);
               return null;
            }
         }).when(GitUtils.class);
         GitUtils.goToTag(Mockito.anyString(), Mockito.any(File.class));
         
         final JUnitTestTransformer transformer = new JUnitTestTransformer(projectFolder);
         final MeasurementConfiguration config = new MeasurementConfiguration(2);
         executor = new CauseTester(new PeASSFolders(projectFolder), transformer, config, TEST);
         LOG.debug("Executor: {}", executor);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static void mockGetVCS() {
      PowerMockito.doAnswer(new Answer<VersionControlSystem>() {

         @Override
         public VersionControlSystem answer(final InvocationOnMock invocation) throws Throwable {
            return VersionControlSystem.GIT;
         }
      }).when(VersionControlSystem.class);
      VersionControlSystem.getVersionControlSystem(Mockito.any(File.class));
   }

   @Test
   public void testOneMethodExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final Set<CallTreeNode> included = new HashSet<>();
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#child1", "public void defaultpackage.NormalDependency.child1()", null);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);

      executor.evaluate("00001", "00001~1", TEST);

      executor.getDurations("00001", "00001~1", 0);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001~1").getN());
   }
   
   @Test
   public void testConstructorExecution() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final Set<CallTreeNode> included = new HashSet<>();
      final CallTreeNode nodeWithDuration = new CallTreeNode("defaultpackage.NormalDependency#<init>", "public new defaultpackage.NormalDependency.<init>()", null);
      included.add(nodeWithDuration);
      executor.setIncludedMethods(included);

      executor.evaluate("00001", "00001~1", TEST);

      executor.getDurations("00001", "00001~1", 1);

      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001").getN());
      Assert.assertEquals(2, nodeWithDuration.getStatistics("00001~1").getN());
   }

   public void testMultipleMethodExecution() {
      // TODO Auto-generated method stub
   }
}
