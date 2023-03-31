package de.dagere.peass.dependencytests;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.Type;
import de.dagere.nodeDiffDetector.diffDetection.ClazzChangeData;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIterator;

public class DependencyDetectorIT {

   @BeforeEach
   public void initialize() throws Exception {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);

   }

   @Test
   public void testNormalChange() throws Exception {

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(DependencyTestConstants.NORMAL_CHANGE));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }

   @Test
   public void testNoChange() throws Exception {

      final Map<Type, ClazzChangeData> changes = new HashMap<>();
      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(DependencyTestConstants.BASIC_STATE));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      StaticTestSelection dependencies = reader.getDependencies();
      System.out.println(dependencies.getCommits());

      Assert.assertTrue(dependencies.getCommits().get(dependencies.getNewestCommit()).isRunning());

      // DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }

   @Test
   public void testAddedTest() throws Exception {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "added_test");
      final ChangeManager changeManager = DependencyDetectorTestUtil.changedTestClassChangeManager();

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      DependencyDetectorTestUtil.checkChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1, "addedTest");
   }

   @Test
   public void testTestChange() throws Exception {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "changed_test");

      final Map<Type, ClazzChangeData> changes = new TreeMap<>();
      DependencyDetectorTestUtil.addChange(changes, "", "defaultpackage.TestMe", "testMe");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies().getCommits().get(DependencyTestConstants.VERSION_1));

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.TestMe#testMe", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }

   @Test
   public void testAddedClass() throws Exception {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "added_class");

      final ChangeManager changeManager = DependencyDetectorTestUtil.mockAddedChangeManager();

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.TestMeAlso", "defaultpackage.TestMeAlso", DependencyTestConstants.VERSION_1);
   }

   @Test
   public void testClassChange() throws Exception {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "changed_class");

      final Map<Type, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new Type("defaultpackage.NormalDependency", ""), new ClazzChangeData("defaultpackage.NormalDependency", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(DependencyTestConstants.CURRENT),
            DependencyTestConstants.NULL_RESULTS_FOLDERS, null, fakeIterator, changeManager, new ExecutionConfig(5), new KiekerConfig(true), new EnvironmentVariables());
      final boolean success = reader.readInitialCommit();
      Assert.assertTrue(success);

      final StaticTestSelection dependencies = reader.getDependencies();
      System.out.println(dependencies);

      fakeIterator.goToNextCommit();

      reader.analyseCommit(changeManager);

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(dependencies, "defaultpackage.NormalDependency", DependencyTestConstants.VERSION_1);

      System.out.println(testMe);
      final Type change = dependencies.getCommits().get(DependencyTestConstants.VERSION_1).getChangedClazzes().keySet().iterator().next();
      Assert.assertEquals("defaultpackage.NormalDependency", change.toString());
      Assert.assertEquals("defaultpackage.TestMe#testMe", testMe.getTestMethods().iterator().next().getExecutable());
   }

   /**
    * Tests removal of a method. In the first version, the method should not be called (but the other method of TestMe should be called, since the class interface changed). In the
    * second version, the changes should only influence TestMe.testMe, not TestMe.removeMe.
    * 
    * @throws Exception
    */
   @Test
   public void testMethodRemoval() throws Exception {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_method");
      final File thirdVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_method_change");

      final Map<Type, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new Type("defaultpackage.TestMe", ""), new ClazzChangeData("defaultpackage.TestMe", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion, thirdVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      Assert.assertEquals(1, reader.getDependencies().getCommits().get("000001").getChangedClazzes().size());

      fakeIterator.goToNextCommit();
      reader.analyseCommit(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.TestMe", DependencyTestConstants.VERSION_2);

      final TestMethodCall test = testMe.getTestMethods().iterator().next();
      Assert.assertEquals(1, testMe.getTestMethods().size());
      Assert.assertEquals("defaultpackage.TestMe", test.getClazz());
      Assert.assertEquals("testMe", test.getMethod());
   }

   @Test
   public void testClassRemoval() throws Exception {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_class");

      final Map<Type, ClazzChangeData> changes = new TreeMap<>();
      final Type changedEntity = new Type("defaultpackage.TestMe", "");
      changes.put(changedEntity, new ClazzChangeData(changedEntity, false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      checkClassRemoved(reader);
   }

   public static void checkClassRemoved(final DependencyReader reader) {
      final Map<Type, TestSet> changedClazzes = reader.getDependencies().getCommits().get(DependencyTestConstants.VERSION_1).getChangedClazzes();
      System.out.println("Ergebnis: " + changedClazzes);
      final Type key = new Type("defaultpackage.TestMe", "");
      System.out.println("Hash: " + key.hashCode());
      final TestSet testSet = changedClazzes.get(key);
      System.out.println("Testset: " + testSet);
      MatcherAssert.assertThat(testSet.getTestMethods(), Matchers.empty());
   }
}
