package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.InitialDependency;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.vcs.VersionIterator;

public class DependencyDetectorIncludesTest {
   @Before
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
   }

   @Test
   public void testNormalChangeIncluded() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "normal_change");
      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();
      final DependencyReader reader = executeWithInclude("defaultpackage.TestMe#testMe", secondVersion, changeManager);
      checkContainsOnlyTestMe(reader);
   }
   
   @Test
   public void testNormalChangeAddedClass() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "added_class");
      final ChangeManager changeManager = DependencyDetectorTestUtil.mockAddedChangeManager();
      final DependencyReader reader = executeWithInclude("defaultpackage.TestMe#testMe", secondVersion, changeManager);
      checkContainsOnlyTestMeNoAddition(reader);
   }

   @Test
   public void testNormalChangeNotIncluded() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "normal_change");
      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();
      final DependencyReader reader = executeWithInclude("defaultpackage.TestMe#removeMe", secondVersion, changeManager);
      checkContainsOnlyRemoveMe(reader);
   }

   private void checkContainsOnlyRemoveMe(final DependencyReader reader) {
      checkInitialDependency(reader, "testMe");

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      Assert.assertEquals(0, testMe.getTests().size());
   }
   
   private void checkContainsOnlyTestMe(final DependencyReader reader) {
      checkInitialDependency(reader, "removeMe");

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
      
      TestSet tests = reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1).getTests();
      System.out.println(tests);
      Assert.assertEquals("Added class should not be selected since it is not included", 1, tests.entrySet().size());
   }

   private void checkContainsOnlyTestMeNoAddition(final DependencyReader reader) {
      checkInitialDependency(reader, "removeMe");

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
      
      TestSet tests = reader.getDependencies().getVersions().get(DependencyTestConstants.VERSION_1).getTests();
      System.out.println(tests);
      Assert.assertEquals("Added class should not be selected since it is not included", 1, tests.entrySet().size());
   }
   
   private void checkInitialDependency(final DependencyReader reader, final String notIncludedMethod) {
      System.out.println(reader.getDependencies());
      
      Map<ChangedEntity, InitialDependency> initialDependencies = reader.getDependencies().getInitialversion().getInitialDependencies();
      ChangedEntity removeMeEntity = new ChangedEntity("defaultpackage.TestMe", "", notIncludedMethod);
      System.out.println(initialDependencies.get(removeMeEntity));
      Assert.assertNull(initialDependencies.get(removeMeEntity));
   }

   
   private DependencyReader executeWithInclude(final String includeName, final File secondVersion, final ChangeManager changeManager) throws IOException, InterruptedException, XmlPullParserException {
      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      ExecutionConfig config = new ExecutionConfig(5);
     
      config.getIncludes().add(includeName);
      
      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator, config);
      return reader;
   }
   
}
