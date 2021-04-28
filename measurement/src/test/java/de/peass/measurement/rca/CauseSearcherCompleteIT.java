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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitUtils.class, VersionControlSystem.class})
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class CauseSearcherCompleteIT {
   
   private static final Logger LOG = LogManager.getLogger(CauseSearcherCompleteIT.class);
   
   public static final File CURRENT = TestConstants.getCurrentFolder();
   private static final File VERSIONS_FOLDER = new File("src/test/resources/rootCauseIT");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state_inheritance");
   private static final File SLOW_STATE = new File(VERSIONS_FOLDER, "slow_state_inheritance");

   @Before
   public void setUp() throws InterruptedException, IOException {
      try {
         FileUtils.copyDirectory(BASIC_STATE, CURRENT);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      
      final PeASSFolders folders = new PeASSFolders(CURRENT);
      final File projectFolderTemp = new File(folders.getTempProjectFolder(), "000001");
      
      VCSTestUtils.mockGetVCS();
      
      PowerMockito.mockStatic(GitUtils.class);
      VCSTestUtils.mockClone(projectFolderTemp, CURRENT);
      
      VCSTestUtils.mockGoToTag(folders, SLOW_STATE, BASIC_STATE);
   }

   @Ignore
   @Test
   public void testSlowerState() throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(2, "000001", "000001~1");
      measurementConfiguration.setUseKieker(true);
      final CauseSearchFolders folders = new CauseSearchFolders(CURRENT);
      final BothTreeReader reader = new BothTreeReader(TestConstants.SIMPLE_CAUSE_CONFIG, measurementConfiguration, folders, new EnvironmentVariables());
      final CauseTester measurer = new CauseTester(folders, measurementConfiguration, TestConstants.SIMPLE_CAUSE_CONFIG, new EnvironmentVariables());
      final CauseSearcher searcher = new LevelCauseSearcher(reader, TestConstants.SIMPLE_CAUSE_CONFIG, measurer, measurementConfiguration, folders, new EnvironmentVariables());
      final Set<ChangedEntity> changedEntities = searcher.search();

      LOG.debug(changedEntities);
      Assert.assertThat(changedEntities.size(), Matchers.greaterThanOrEqualTo(1));
      Assert.assertEquals("defaultpackage.NormalDependency#child12", changedEntities.iterator().next().toString());
      
   }

   // child12
}
