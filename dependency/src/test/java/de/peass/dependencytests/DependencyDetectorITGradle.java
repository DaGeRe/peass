package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.vcs.VersionIterator;
import de.peass.dependencytests.helper.FakeFileIterator;

public class DependencyDetectorITGradle {

   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT_gradle");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");
   private static final File BASIC_STATE_ANDROID = new File(VERSIONS_FOLDER, "basic_state_android");
   private static final File CHANGE = new File(VERSIONS_FOLDER, "normal_change");
   private static final File CHANGE_ANDROID = new File(VERSIONS_FOLDER, "normal_change_android");
   

   public void init(final File folder) throws IOException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());
      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FakeFileIterator.copy(folder, DependencyTestConstants.CURRENT);
   }

   // @org.junit.After
   // public void cleanAfterwards() throws IOException {
   // FileUtils.deleteDirectory(CURRENT);
   // // be aware: maven does not compile if a .class-file is still in the resources, since it gets identified as test
   // }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
      init(BASIC_STATE);

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(CHANGE));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }
   
   @Test
   public void testNormalChangeAndroid() throws IOException, InterruptedException, XmlPullParserException {
      init(BASIC_STATE_ANDROID);
      
      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(CHANGE_ANDROID));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }


}
