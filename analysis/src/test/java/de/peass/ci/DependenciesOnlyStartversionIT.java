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

import de.peass.TestConstants;
import de.peass.ci.helper.GitProjectBuilder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencytests.helper.FakeFileIterator;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DependenciesOnlyStartversionIT {

   private static GitProjectBuilder builder;

   Dependencies dependencies;
   
   @BeforeEach
   public void cleanDependencies() throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      TestContinuousDependencyReader.dependencyFile.delete();
      Assert.assertFalse(TestContinuousDependencyReader.dependencyFile.exists());

      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      builder = new GitProjectBuilder(TestConstants.CURRENT_FOLDER, new File("../dependency/src/test/resources/dependencyIT/basic_state"));
      
      VersionIterator iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);
      iterator.goToFirstCommit();
      
      ContinuousDependencyReader reader = new ContinuousDependencyReader(iterator.getTag(), TestConstants.CURRENT_FOLDER, TestContinuousDependencyReader.dependencyFile);
      dependencies = reader.getDependencies(iterator, "");
      
      Assert.assertEquals(0, dependencies.getVersions().size());
   }

   @Order(1)
   @Test
   public void testBasicVersionReading() throws JsonParseException, JsonMappingException, JAXBException, IOException, InterruptedException, XmlPullParserException {
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/changed_class"), "test 1");
      
      VersionIterator iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);
      iterator.goToFirstCommit();
      iterator.goToNextCommit();
      
      ContinuousDependencyReader reader = new ContinuousDependencyReader(iterator.getTag(), TestConstants.CURRENT_FOLDER, TestContinuousDependencyReader.dependencyFile);
      dependencies = reader.getDependencies(iterator, "");

      final String lastTag = builder.getTags().get(builder.getTags().size() - 1);
      checkVersion(dependencies, lastTag, 1);
   }


   private void checkVersion(Dependencies dependencies, final String newestVersion, int versions) {
      Assert.assertTrue(TestContinuousDependencyReader.dependencyFile.exists());
      MatcherAssert.assertThat(dependencies.getVersions(), Matchers.aMapWithSize(versions));

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
