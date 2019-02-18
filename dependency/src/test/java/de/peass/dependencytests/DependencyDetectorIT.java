package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.ClazzChangeData;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.reader.DependencyReader;
import de.peass.vcs.VersionIterator;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest( VersionControlSystem.class )
public class DependencyDetectorIT {

   static final String VERSION_1 = "000001";
   static final String VERSION_2 = "000002";
   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   public static final File CURRENT = new File(new File("target"), "current");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

   @Before
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(CURRENT);
      FileUtils.copyDirectory(BASIC_STATE, CURRENT);
      
   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
//      PowerMockito.mockStatic(VersionControlSystem.class);
//      PowerMockito.doAnswer(new Answer<Void>() {
//
//         @Override
//         public Void answer(final InvocationOnMock invocation) throws Throwable {
//            System.out.println("Changed!");
//            return null;
//         }
//      }).when(VersionControlSystem.class);
      
      final File secondVersion = new File(VERSIONS_FOLDER, "normal_change");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ClazzChangeData methodChanges = new ClazzChangeData("defaultpackage.NormalDependency");
      methodChanges.getChangedMethods().add("executeThing");
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), methodChanges);

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);
      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   @Test
   public void testTestChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(VERSIONS_FOLDER, "changed_test");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      final ClazzChangeData methodChanges = new ClazzChangeData("defaultpackage.NormalDependency");
      methodChanges.getChangedMethods().add("testMe");
      changes.put(new ChangedEntity("defaultpackage.TestMe", ""), methodChanges);

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);
      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = findDependency(reader.getDependencies(), "defaultpackage.TestMe#testMe", VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   static TestSet findDependency(final Dependencies dependencies, final String changedClass, final String version) {
      final Version secondVersionDependencies = dependencies.getVersions().get(version);
//      for (final Version candidate : dependencies.getVersions().getVersion()) {
//         if (candidate.getVersion().equals(version)) {
//            secondVersionDependencies = candidate;
//         }
//      }
      Assert.assertNotNull("Searching for " + changedClass + " in " + version, secondVersionDependencies);

      TestSet testcase = null;
      for (final Entry<ChangedEntity, TestSet> candidate : secondVersionDependencies.getChangedClazzes().entrySet()) {
         final String changeclassInDependencies = candidate.getKey().toString();
         if (changeclassInDependencies.equals(changedClass)) {
            testcase = candidate.getValue();
         }
      }
      return testcase;
   }

   @Test
   public void testAddedClass() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(VERSIONS_FOLDER, "added_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), new ClazzChangeData("defaultpackage.NormalDependency", false));
      changes.put(new ChangedEntity("defaultpackage.TestMeAlso", ""), new ClazzChangeData("defaultpackage.TestMeAlso", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      System.out.println(new ObjectMapper().writeValueAsString(reader.getDependencies()));

      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMeAlso = findDependency(reader.getDependencies(), "defaultpackage.TestMeAlso", VERSION_1);
      final TestCase testcase = testMeAlso.getTests().iterator().next();

      System.out.println(testMeAlso);
      Assert.assertEquals("defaultpackage.TestMeAlso", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

   @Test
   public void testClassChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(VERSIONS_FOLDER, "changed_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.NormalDependency", ""), new ClazzChangeData("defaultpackage.NormalDependency", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      final Dependencies dependencies = reader.getDependencies();
      System.out.println(dependencies);

      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(dependencies);

      final TestSet testMe = findDependency(dependencies, "defaultpackage.NormalDependency", VERSION_1);

      System.out.println(testMe);
      final ChangedEntity change = dependencies.getVersions().get(VERSION_1).getChangedClazzes().keySet().iterator().next();
      Assert.assertEquals("defaultpackage.NormalDependency", change.toString());
      Assert.assertEquals("defaultpackage.TestMe#testMe", testMe.getTests().iterator().next().getExecutable());
   }

   /**
    * Tests removal of a method. In the first version, the method should not be called (but the other method of TestMe should be called, since the class interface changed). In the
    * second version, the changes should only influence TestMe.testMe, not TestMe.removeMe.
    * 
    * @throws IOException
    * @throws InterruptedException
    * @throws XmlPullParserException
    */
   @Test
   public void testRemoval() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(VERSIONS_FOLDER, "removed_method");
      final File thirdVersion = new File(VERSIONS_FOLDER, "removed_method_change");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("defaultpackage.TestMe", ""), new ClazzChangeData("defaultpackage.TestMe", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion, thirdVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      fakeIterator.goToNextCommit();
      reader.analyseVersion(changeManager);
      System.out.println(reader.getDependencies());

      Assert.assertEquals(1, reader.getDependencies().getVersions().get("000001").getChangedClazzes().size());

      fakeIterator.goToNextCommit();
      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = findDependency(reader.getDependencies(), "defaultpackage.TestMe", VERSION_2);

      final TestCase test = testMe.getTests().iterator().next();
      Assert.assertEquals(1, testMe.getTests().size());
      Assert.assertEquals("defaultpackage.TestMe", test.getClazz());
      Assert.assertEquals("testMe", test.getMethod());
   }
   
   @Test
   public void testClassRemoval() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(VERSIONS_FOLDER, "removed_class");

      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      changes.put(new ChangedEntity("src/test/java/defaultpackage/TestMe.java", ""), new ClazzChangeData("defaultpackage.TestMe", false));

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);

      final VersionIterator fakeIterator = new FakeIterator(CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);

      fakeIterator.goToNextCommit();
      reader.analyseVersion(changeManager);
      System.out.println(reader.getDependencies());

      final Map<ChangedEntity, TestSet> changedClazzes = reader.getDependencies().getVersions().get(VERSION_1).getChangedClazzes();
      System.out.println("Ergebnis: " + changedClazzes);
      final ChangedEntity key = new ChangedEntity("defaultpackage.TestMe", "");
      System.out.println("Hash: " + key.hashCode());
      final TestSet testSet = changedClazzes.get(key);
      System.out.println("Testset: " + testSet);
      Assert.assertThat(testSet.getTests(), Matchers.empty());
   }

   @Test
   public void testPackageChange() {

   }
}
