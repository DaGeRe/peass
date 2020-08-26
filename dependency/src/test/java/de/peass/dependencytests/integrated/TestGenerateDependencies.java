package de.peass.dependencytests.integrated;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencytests.ViewGeneratorIT;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VersionControlSystem.class)
@PowerMockIgnore({ "javax.management.*", "org.apache.http.conn.ssl.*", "com.amazonaws.http.conn.ssl.*", "javax.net.ssl.*" })
public class TestGenerateDependencies {

   @Test
   public void testGenerateDependencies() throws IOException, InterruptedException, XmlPullParserException {
      ViewGeneratorIT.init(ViewGeneratorIT.BASIC);

      PowerMockito.mockStatic(VersionControlSystem.class);
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            System.out.println("Changed!");
            return null;
         }
      }).when(VersionControlSystem.class);

      final FakeFileIterator iterator = new FakeFileIterator(ViewGeneratorIT.projectFolder, Arrays.asList(ViewGeneratorIT.REPETITION));
      final File dependencyFile = new File(ViewGeneratorIT.VIEW_IT, "dependencies.json");
      final DependencyReader reader = new DependencyReader(ViewGeneratorIT.projectFolder, dependencyFile, "", iterator, 5000, VersionKeeper.INSTANCE);

      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      iterator.goToNextCommit();

      final ChangeManager manager = Mockito.mock(ChangeManager.class);
      final HashMap<ChangedEntity, ClazzChangeData> value = new HashMap<>();
      ChangedEntity testMeEntity = new ChangedEntity("viewtest.TestMe", "");
      value.put(testMeEntity, new ClazzChangeData(testMeEntity, false));
      Mockito.when(manager.getChanges(Mockito.any())).thenReturn(value);

      final int tests = reader.analyseVersion(manager);
      DependencyReaderUtil.write(reader.getDependencies(), dependencyFile);
      Assert.assertEquals(1, tests);
   }
}
