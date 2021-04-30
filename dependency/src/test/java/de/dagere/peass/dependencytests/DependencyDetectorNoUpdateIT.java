package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.vcs.VersionIterator;

public class DependencyDetectorNoUpdateIT {

   @Before
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);

   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "normal_change");

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies());

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.NormalDependency#executeThing", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }

   @Test
   public void testTestChange() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "changed_test");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      DependencyDetectorTestUtil.addChange(changes, "", "defaultpackage.TestMe", "testMe");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1));

      DependencyDetectorTestUtil.checkTestMeAlsoTestChange(reader, "defaultpackage.TestMe#testMe", "defaultpackage.TestMe", DependencyTestConstants.VERSION_1);
   }

   @Test
   public void testClassRemoval() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "removed_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ChangedEntity changedEntity = new ChangedEntity("src/test/java/defaultpackage/TestMe.java", "");
      changes.put(changedEntity, new ClazzChangeData(changedEntity, false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      DependencyDetectorIT.checkClassRemoved(reader);
   }

}
