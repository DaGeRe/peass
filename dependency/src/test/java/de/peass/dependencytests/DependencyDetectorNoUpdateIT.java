package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.config.DependencyConfig;
import de.peass.config.ExecutionConfig;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionIterator;

public class DependencyDetectorNoUpdateIT {

   private static final DependencyConfig dependencyConfig = new DependencyConfig(1, true);

   @Before
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);

   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "normal_change");

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   @Test
   public void testTestChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "changed_test");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      DependencyDetectorTestUtil.addChange(changes, "", "defaultpackage.TestMe", "testMe");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1));

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.TestMe#testMe", DependencyTestConstants.VERSION_1);
      System.out.println(testMe);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   @Test
   public void testClassRemoval() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ChangedEntity changedEntity = new ChangedEntity("src/test/java/defaultpackage/TestMe.java", "");
      changes.put(changedEntity, new ClazzChangeData(changedEntity, false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = readTwoVersions(changeManager, fakeIterator);

      final Map<ChangedEntity, TestSet> changedClazzes = reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1).getChangedClazzes();
      System.out.println("Ergebnis: " + changedClazzes);
      final ChangedEntity key = new ChangedEntity("defaultpackage.TestMe", "");
      System.out.println("Hash: " + key.hashCode());
      final TestSet testSet = changedClazzes.get(key);
      System.out.println("Testset: " + testSet);
      Assert.assertThat("TestSet needs to contain removed test, since no update has been done", testSet.getTests(), Matchers.not(Matchers.empty()));
   }

   public static DependencyReader readTwoVersions(final ChangeManager changeManager, final VersionIterator fakeIterator)
         throws IOException, InterruptedException, XmlPullParserException {
      final DependencyReader reader = new DependencyReader(dependencyConfig, new PeASSFolders(DependencyTestConstants.CURRENT), new File("/dev/null"), null, fakeIterator,
            changeManager, new ExecutionConfig(5), new EnvironmentVariables());
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);
      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);
      return reader;
   }
}
