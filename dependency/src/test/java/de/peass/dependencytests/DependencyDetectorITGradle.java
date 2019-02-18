package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.ClazzChangeData;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.reader.DependencyReader;
import de.peass.vcs.VersionIterator;

public class DependencyDetectorITGradle {

   static final String VERSION_1 = "000001";
   static final String VERSION_2 = "000002";
   private static final File CURRENT = new File(new File("target"), "current");
   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT_gradle");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");
   private static final File BASIC_STATE_ANDROID = new File(VERSIONS_FOLDER, "basic_state_android");
   private static final File CHANGE = new File(VERSIONS_FOLDER, "normal_change");
   private static final File CHANGE_ANDROID = new File(VERSIONS_FOLDER, "normal_change_android");
   

   public void init(final File folder) throws IOException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());
      FileUtils.deleteDirectory(CURRENT);
      FakeIterator.copy(folder, CURRENT);
   }

   // @org.junit.After
   // public void cleanAfterwards() throws IOException {
   // FileUtils.deleteDirectory(CURRENT);
   // // be aware: maven does not compile if a .class-file is still in the resources, since it gets identified as test
   // }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
      init(BASIC_STATE);

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ClazzChangeData methodChanges = new ClazzChangeData("defaultpackage.NormalDependency");
      methodChanges.getChangedMethods().add("executeThing");
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), methodChanges);

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(CHANGE));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);
      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorIT.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency.executeThing", VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }
   
   @Test
   public void testNormalChangeAndroid() throws IOException, InterruptedException, XmlPullParserException {
      init(BASIC_STATE_ANDROID);
      
      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ClazzChangeData methodChanges = new ClazzChangeData("defaultpackage.NormalDependency");
      methodChanges.getChangedMethods().add("executeThing");
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), methodChanges);

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(CHANGE_ANDROID));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);
      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorIT.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency.executeThing", VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }


}
