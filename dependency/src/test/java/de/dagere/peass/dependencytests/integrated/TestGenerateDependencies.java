package de.dagere.peass.dependencytests.integrated;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.reader.CommitKeeper;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.DependencyDetectorTestUtil;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.dependencytests.TraceGettingIT;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class TestGenerateDependencies {

   private static final Logger LOG = LogManager.getLogger(TestGenerateDependencies.class);

   @Test
   public void testGenerateDependencies() throws IOException, InterruptedException, XmlPullParserException, ParseException {
      DependencyDetectorTestUtil.init(TraceGettingIT.BASIC);

      final FakeFileIterator iterator = new FakeFileIterator(TestConstants.CURRENT_FOLDER, Arrays.asList(TraceGettingIT.REPETITION));
      ResultsFolders resultsFolders = new ResultsFolders(TraceGettingIT.VIEW_IT_PROJECTFOLDER, "test");
      final DependencyReader reader = new DependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(TestConstants.CURRENT_FOLDER), resultsFolders, "", iterator, CommitKeeper.INSTANCE, new ExecutionConfig(5), new KiekerConfig(true), new EnvironmentVariables());

      final boolean success = reader.readInitialCommit();
      Assert.assertTrue(success);

      iterator.goToNextCommit();

      final ChangeManager manager = Mockito.mock(ChangeManager.class);
      final HashMap<MethodCall, ClazzChangeData> value = new HashMap<>();
      MethodCall testMeEntity = new MethodCall("viewtest.TestMe", "");
      value.put(testMeEntity, new ClazzChangeData(testMeEntity, false));
      Mockito.when(manager.getChanges(Mockito.any())).thenReturn(value);

      final int tests = reader.analyseCommit(manager);

      LOG.debug(Constants.OBJECTMAPPER.writeValueAsString(reader.getDependencies()));

      Assert.assertEquals(1, tests);
   }
}
