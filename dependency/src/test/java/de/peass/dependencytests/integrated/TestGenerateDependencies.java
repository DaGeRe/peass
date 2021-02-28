package de.peass.dependencytests.integrated;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.TestConstants;
import de.peass.config.ExecutionConfig;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencytests.ViewGeneratorIT;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.utils.Constants;

public class TestGenerateDependencies {

   private static final Logger LOG = LogManager.getLogger(TestGenerateDependencies.class);

   @Test
   public void testGenerateDependencies() throws IOException, InterruptedException, XmlPullParserException {
      ViewGeneratorIT.init(ViewGeneratorIT.BASIC);

      final FakeFileIterator iterator = new FakeFileIterator(TestConstants.CURRENT_FOLDER, Arrays.asList(ViewGeneratorIT.REPETITION));
      final File dependencyFile = new File(ViewGeneratorIT.VIEW_IT, "dependencies.json");
      final DependencyReader reader = new DependencyReader(TestConstants.CURRENT_FOLDER, dependencyFile, "", iterator, VersionKeeper.INSTANCE, new ExecutionConfig(5));

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
