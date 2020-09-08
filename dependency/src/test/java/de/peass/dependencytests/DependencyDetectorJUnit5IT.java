package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionIterator;

@Disabled
public class DependencyDetectorJUnit5IT {

   static final String VERSION_1 = "000001";
   static final String VERSION_2 = "000002";
   private static final File VERSIONS_FOLDER = new File("src/test/resources/dependencyIT");
   public static final File CURRENT = new File(new File("target"), "current");
   private static final File BASIC_STATE = new File(VERSIONS_FOLDER, "basic_state");

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(CURRENT);
      FileUtils.copyDirectory(BASIC_STATE, CURRENT);
      
   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException {
      final File secondVersion = new File(VERSIONS_FOLDER, "normal_change");

      final ChangeManager changeManager = DependencyDetectorTestUtil.defaultChangeManager();

      final VersionIterator fakeIterator = new FakeFileIterator(CURRENT, Arrays.asList(secondVersion));

      final DependencyReader reader = new DependencyReader(CURRENT, new File("/dev/null"), null, fakeIterator, 5000, changeManager);
      final boolean success = reader.readInitialVersion();
      Assert.assertTrue(success);
      fakeIterator.goToNextCommit();

      reader.analyseVersion(changeManager);

      System.out.println(reader.getDependencies());

      final TestSet testMe = DependencyDetectorTestUtil.findDependency(reader.getDependencies(), "defaultpackage.NormalDependency#executeThing", VERSION_1);
      final TestCase testcase = testMe.getTests().iterator().next();
      Assert.assertEquals("defaultpackage.TestMe", testcase.getClazz());
      Assert.assertEquals("testMe", testcase.getMethod());
   }

}
