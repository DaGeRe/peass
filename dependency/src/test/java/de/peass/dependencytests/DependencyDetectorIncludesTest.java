package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.peass.config.ExecutionConfig;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.InitialDependency;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionIterator;

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

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      ExecutionConfig config = new ExecutionConfig(5);
      config.getIncludes().add("defaultpackage.TestMe#testMe");
      
      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator, config);

      checkContainsOnlyTestMe(reader);
   }

   private void checkContainsOnlyTestMe(final DependencyReader reader) {
      System.out.println(reader.getDependencies());
      
      Map<ChangedEntity, InitialDependency> initialDependencies = reader.getDependencies().getInitialversion().getInitialDependencies();
      ChangedEntity removeMeEntity = new ChangedEntity("defaultpackage.TestMe", "", "removeMe");
      System.out.println(initialDependencies.get(removeMeEntity));
      Assert.assertNull(initialDependencies.get(removeMeEntity));

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }
   
   @Test
   public void testNormalChangeNotIncluded() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(DependencyTestConstants.VERSIONS_FOLDER, "normal_change");

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(secondVersion));

      ExecutionConfig config = new ExecutionConfig(5);
      config.getIncludes().add("defaultpackage.TestMe#removeMe");
      
      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator, config);

      checkContainsOnlyRemoveMe(reader);
   }

   private void checkContainsOnlyRemoveMe(final DependencyReader reader) {
      System.out.println(reader.getDependencies());
      
      Map<ChangedEntity, InitialDependency> initialDependencies = reader.getDependencies().getInitialversion().getInitialDependencies();
      ChangedEntity removeMeEntity = new ChangedEntity("defaultpackage.TestMe", "", "testMe");
      System.out.println(initialDependencies);
      Assert.assertNull(initialDependencies.get(removeMeEntity));

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", DependencyTestConstants.VERSION_1);
      Assert.assertEquals(0, testMe.getTests().size());
   }
   
}
