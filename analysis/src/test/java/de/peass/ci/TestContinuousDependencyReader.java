package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.ci.helper.GitProjectBuilder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestContinuousDependencyReader {

   public static final File CURRENT = new File("target", "current");

   private static final File dependencyFile = new File("target", "dependencies.json");

   private final GitProjectBuilder builder;
   
   public TestContinuousDependencyReader() throws InterruptedException, IOException {
      FileUtils.deleteDirectory(CURRENT);
      builder = new GitProjectBuilder(CURRENT, new File("../dependency/src/test/resources/dependencyIT/basic_state"));
   }
   
   @BeforeAll
   public static void cleanDependencies() {
      dependencyFile.delete();
      Assert.assertFalse(dependencyFile.exists());
   }

   @Order(1)
   @Test
   public void testBasicVersionReading() throws JsonParseException, JsonMappingException, JAXBException, IOException, InterruptedException, XmlPullParserException {
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/changed_class"), "test 1");
      
      VersionIterator iterator = new VersionIteratorGit(CURRENT);
      iterator.goToFirstCommit();
      iterator.goToNextCommit();

      ContinuousDependencyReader reader = new ContinuousDependencyReader(iterator.getTag(), CURRENT, dependencyFile);
      Dependencies dependencies = reader.getDependencies(iterator, "");
      
      final String lastTag = builder.getTags().get(builder.getTags().size()-1);
      checkVersion(dependencies, lastTag);
   }
   
   private void checkVersion(Dependencies dependencies, final String newestVersion) {
      Assert.assertTrue(dependencyFile.exists());
      MatcherAssert.assertThat(dependencies.getVersions(), Matchers.aMapWithSize(1));
      
      MatcherAssert.assertThat(dependencies.getVersions().get(newestVersion), Matchers.notNullValue());
      final TestSet testSet = getTestset(dependencies, newestVersion);
      Assert.assertEquals(new TestCase("defaultpackage.TestMe#testMe"), testSet.getTests().toArray()[0]);
   }
   
   private TestSet getTestset(Dependencies dependencies, String newestVersion) {
      final TestSet testSet = dependencies.getVersions().get(newestVersion)
            .getChangedClazzes()
            .get(new ChangedEntity("defaultpackage.NormalDependency", "", ""));
      return testSet;
   }
}
