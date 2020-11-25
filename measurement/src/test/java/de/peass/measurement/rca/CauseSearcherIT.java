package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
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

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitUtils.class, VersionControlSystem.class})
@PowerMockIgnore({"javax.management.*"})
public class CauseSearcherIT {
   
   private static final Logger LOG = LogManager.getLogger(CauseSearcherIT.class);
   
   public final static CauseSearcherConfig CAUSE_CONFIG_TESTME_COMPLETE = new CauseSearcherConfig(new TestCase("defaultpackage.TestMe", "testMe"), 
         false, false, 5.0, false, 0.1,
         false, false, RCAStrategy.COMPLETE);
   
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
      } catch (final IOException e) {
         e.printStackTrace();
      }
      
      final PeASSFolders folders = new PeASSFolders(CURRENT);
      final File projectFolderTemp = new File(folders.getTempProjectFolder(), "000001");
      
      VCSTestUtils.mockGetVCS();
      
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
            final File destFile = (File) invocation.getArgument(1);
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
            final File destFile = (File) invocation.getArgument(1);
            LOG.debug("Loading slower..");
            FileUtils.copyDirectory(SLOW_STATE, destFile);
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.goToTag(Mockito.eq("000001~1"), Mockito.any(File.class));
   }

   @Test
   public void testSlowerState() throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(2, "000001", "000001~1");
      measurementConfiguration.setUseKieker(true);
      final JUnitTestTransformer testTransformer = new JUnitTestTransformer(CURRENT, measurementConfiguration);
      final CauseSearcherConfig causeSearcherConfig = CAUSE_CONFIG_TESTME_COMPLETE;
      
      final CauseSearchFolders folders = new CauseSearchFolders(CURRENT);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders);
      final CauseTester measurer = new CauseTester(folders, testTransformer, causeSearcherConfig);
      final CauseSearcher searcher = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, folders);
      final Set<ChangedEntity> changedEntities = searcher.search();

      LOG.debug(changedEntities);
      Assert.assertThat(changedEntities.size(), Matchers.greaterThanOrEqualTo(1));
      Assert.assertEquals("defaultpackage.NormalDependency#child12", changedEntities.iterator().next().toString());
      
   }

   // child12
}
