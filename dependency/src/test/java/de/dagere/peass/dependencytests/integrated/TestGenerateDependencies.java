package de.dagere.peass.dependencytests.integrated;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.DependencyDetectorTestUtil;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.dependencytests.TraceGettingIT;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.utils.Constants;

public class TestGenerateDependencies {

   private static final Logger LOG = LogManager.getLogger(TestGenerateDependencies.class);

   @Test
   public void testGenerateDependencies() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      DependencyDetectorTestUtil.init(TraceGettingIT.BASIC);

      final FakeFileIterator iterator = new FakeFileIterator(TestConstants.CURRENT_FOLDER, Arrays.asList(TraceGettingIT.REPETITION));
      ResultsFolders resultsFolders = new ResultsFolders(TraceGettingIT.VIEW_IT_PROJECTFOLDER, "test");
      final DependencyReader reader = new DependencyReader(DependencyTestConstants.DEFAULT_CONFIG, new PeASSFolders(TestConstants.CURRENT_FOLDER), resultsFolders, "", iterator, VersionKeeper.INSTANCE, new ExecutionConfig(5), new EnvironmentVariables());

      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      iterator.goToNextCommit();

      final ChangeManager manager = Mockito.mock(ChangeManager.class);
      final HashMap<ChangedEntity, ClazzChangeData> value = new HashMap<>();
      ChangedEntity testMeEntity = new ChangedEntity("viewtest.TestMe", "");
      value.put(testMeEntity, new ClazzChangeData(testMeEntity, false));
      Mockito.when(manager.getChanges(Mockito.any())).thenReturn(value);

      final int tests = reader.analyseVersion(manager);

      LOG.debug(Constants.OBJECTMAPPER.writeValueAsString(reader.getDependencies()));

      Assert.assertEquals(1, tests);
   }
}
