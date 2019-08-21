package de.peass.measurement.searchcause;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.monitoring.core.signaturePattern.PatternParser;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitUtils.class, VersionControlSystem.class})
@PowerMockIgnore("javax.management.*")
public class CauseSearcherCompleteIT {
   
   private static final Logger LOG = LogManager.getLogger(CauseSearcherCompleteIT.class);
   
   public static final File CURRENT = new File(new File("target"), "current");
   private static final File VERSIONS_FOLDER = new File("src/test/resources/rootCauseIT");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");
   private static final File SLOW_STATE = new File(VERSIONS_FOLDER, "slow_state");

   @Before
   public void setUp() throws InterruptedException, IOException {
      try {
         FileUtils.deleteDirectory(CURRENT);
         FileUtils.deleteDirectory(new File(new File("target"), "current_peass"));
         FileUtils.copyDirectory(BASIC_STATE, CURRENT);
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      final PeASSFolders folders = new PeASSFolders(CURRENT);
      final File projectFolderTemp = new File(folders.getTempProjectFolder(), "000001");
      
      PowerMockito.mockStatic(VersionControlSystem.class);
      AdaptiveExecutorTest.mockGetVCS();
      
      PowerMockito.mockStatic(GitUtils.class);
      mockClone(projectFolderTemp);
      
      mockGoToTag(folders, projectFolderTemp);

      
   }

   private void mockClone(final File projectFolderTemp) throws InterruptedException, IOException {
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            FileUtils.copyDirectory(CURRENT, projectFolderTemp);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.clone(Mockito.any(PeASSFolders.class), Mockito.any(File.class));
   }

   private void mockGoToTag(final PeASSFolders folders, final File projectFolderTemp) {
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading faster..");
            FileUtils.deleteDirectory(destFile);
            FileUtils.copyDirectory(BASIC_STATE, destFile);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001"), Mockito.any(File.class));

      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading slower..");
            FileUtils.copyDirectory(SLOW_STATE, destFile);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001~1"), Mockito.any(File.class));
   }

   @Test
   public void testSlowerState() throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final JUnitTestTransformer testgenerator = new JUnitTestTransformer(CURRENT);
      testgenerator.setIterations(30);
      testgenerator.setWarmupExecutions(30);
//      BothTreeReader reader = new Bo
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig("000001", "000001~1", new TestCase("defaultpackage.TestMe", "testMe"));
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(2);
      final PeASSFolders folders = new PeASSFolders(CURRENT);
      BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders);
      LevelMeasurer measurer = new LevelMeasurer(folders, causeSearcherConfig, testgenerator, measurementConfiguration);
      CauseSearcher searcher = new CauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, folders);
      List<ChangedEntity> changedEntities = searcher.search();

      LOG.debug(changedEntities);
      Assert.assertEquals(1, changedEntities.size());
      Assert.assertEquals("defaultpackage.NormalDependency#child12", changedEntities.get(0).toString());
      
   }

   // child12
}
