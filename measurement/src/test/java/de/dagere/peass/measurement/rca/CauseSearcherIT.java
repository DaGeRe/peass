package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.measurement.rca.helper.VCSTestUtils;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.measurement.rca.searcher.CauseSearcherComplete;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitUtils.class, VersionControlSystem.class})
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class CauseSearcherIT {

   private static final Logger LOG = LogManager.getLogger(CauseSearcherIT.class);
   
   private static final TestCase TESTCASE = new TestCase("defaultpackage.TestMe", "testMe");
   public final static CauseSearcherConfig CAUSE_CONFIG_TESTME_COMPLETE = new CauseSearcherConfig(TESTCASE, 
         false, false, 0.1,
         false, false, RCAStrategy.COMPLETE, 1);
   private static final String VERSION = "000001";
   
   private static final File VERSIONS_FOLDER = new File("src/test/resources/rootCauseIT");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");
   private static final File SLOW_STATE = new File(VERSIONS_FOLDER, "slow_state");

   @Before
   public void setUp() throws InterruptedException, IOException {
      try {
         FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
         FileUtils.deleteDirectory(new File(new File("target"), "current_peass"));
         FileUtils.copyDirectory(BASIC_STATE, DependencyTestConstants.CURRENT);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      
      final PeassFolders folders = new PeassFolders(DependencyTestConstants.CURRENT);
      final File projectFolderTemp = new File(folders.getTempProjectFolder(), VERSION);
      
      VCSTestUtils.mockGetVCS();
      
      PowerMockito.mockStatic(GitUtils.class);
      VCSTestUtils.mockClone(projectFolderTemp, DependencyTestConstants.CURRENT);
      VCSTestUtils.mockGoToTag(folders, SLOW_STATE, BASIC_STATE);
   }

   @Test
   public void testSlowerState() throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(5, VERSION, "000001~1");
      measurementConfiguration.setUseKieker(true);
      final CauseSearcherConfig causeSearcherConfig = CAUSE_CONFIG_TESTME_COMPLETE;
      
      final CauseSearchFolders folders = new CauseSearchFolders(DependencyTestConstants.CURRENT);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders, new EnvironmentVariables());
      EnvironmentVariables emptyEnv = new EnvironmentVariables();
      final CauseTester measurer = new CauseTester(folders, measurementConfiguration, causeSearcherConfig, emptyEnv);
      
      final CauseSearcher searcher = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, folders, emptyEnv);
      final Set<ChangedEntity> changedEntities = searcher.search();

      checkChangelistContainsChild12(changedEntities);
      
      File expectedResultLogFolder = folders.getExistingRCALogFolder(VERSION, TESTCASE, 0);
      File expectedResultLogFile = new File(expectedResultLogFolder, "vm_0_" + VERSION);
      
      Assert.assertTrue("File " + expectedResultLogFolder.getAbsolutePath() + " does not exist ", expectedResultLogFolder.exists());
      Assert.assertTrue("File " + expectedResultLogFile.getAbsolutePath() + " does not exist ", expectedResultLogFile.exists());
   }

   /**
    * Child12 needs to be contained - since this is a small test which is executed quickly, other methods may be detected as false positives
    * @param changedEntities
    */
   private void checkChangelistContainsChild12(final Set<ChangedEntity> changedEntities) {
      LOG.debug(changedEntities);
      Assert.assertThat(changedEntities.size(), Matchers.greaterThanOrEqualTo(1));
      List<String> allChanged = changedEntities.stream().map(entity -> entity.toString()).collect(Collectors.toList());
      MatcherAssert.assertThat(allChanged, Matchers.hasItem("defaultpackage.NormalDependency#child12"));
   }
}
