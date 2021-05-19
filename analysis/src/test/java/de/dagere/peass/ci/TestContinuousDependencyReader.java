package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.dagere.peass.TestConstants;
import de.dagere.peass.ci.helper.GitProjectBuilder;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionIteratorGit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestContinuousDependencyReader {

   public static final ResultsFolders resultsFolders = new ResultsFolders(new File("target/results-test"), "test");

   private static GitProjectBuilder builder;

   @BeforeAll
   public static void cleanDependencies() throws IOException, InterruptedException {
      FileUtils.deleteDirectory(resultsFolders.getDependencyFile().getParentFile());
      Assert.assertFalse(resultsFolders.getDependencyFile().exists());

      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      builder = new GitProjectBuilder(TestConstants.CURRENT_FOLDER, new File("../dependency/src/test/resources/dependencyIT/basic_state"));
   }

   @Order(1)
   @Test
   public void testBasicVersionReading() throws Exception {
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/normal_change"), "test 1");

      VersionIteratorGit iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);
      iterator.goToFirstCommit();
      iterator.goToNextCommit();

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setVersion(iterator.getTag());
      executionConfig.setVersionOld(iterator.getPrevious().getTag());

      ContinuousDependencyReader reader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG, executionConfig, new PeASSFolders(TestConstants.CURRENT_FOLDER),
            resultsFolders, new EnvironmentVariables());
      Dependencies dependencies = reader.getDependencies(iterator, "");

      final String lastTag = builder.getTags().get(builder.getTags().size() - 1);
      checkVersion(dependencies, lastTag, 1);
      
      ExecutionData executions = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
      Assert.assertEquals(2, executions.getVersions().size());
      System.out.println(executions.getVersions().keySet());
   }

   @Order(2)
   @Test
   public void testAnotherVersion() throws Exception {
      final String prevTag = builder.getTags().get(builder.getTags().size() - 1);
      GitUtils.goToTag(prevTag, TestConstants.CURRENT_FOLDER);

      String newVersion = builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/basic_state"), "test 2");

      VersionIteratorGit iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setVersion(newVersion);
      executionConfig.setVersionOld(iterator.getPrevious().getTag());

      final ContinuousDependencyReader spiedReader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG, executionConfig,
            new PeASSFolders(TestConstants.CURRENT_FOLDER), resultsFolders, new EnvironmentVariables());
      Dependencies dependencies = spiedReader.getDependencies(iterator, "");

      final String lastTag = builder.getTags().get(builder.getTags().size() - 1);
      checkVersion(dependencies, lastTag, 2);
      
      ExecutionData executions = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
      Assert.assertEquals(3, executions.getVersions().size());
   }
   
   @Disabled
   @Order(3)
   @Test
   public void testEmptyVersion() throws Exception {
      final String prevTag = builder.getTags().get(builder.getTags().size() - 1);
      GitUtils.goToTag(prevTag, TestConstants.CURRENT_FOLDER);

      String newVersion = builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/only_comment_change"), "test 2");
      
      VersionIteratorGit iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setVersion(newVersion);
      executionConfig.setVersionOld(iterator.getPrevious().getTag());

      ContinuousDependencyReader reader = new ContinuousDependencyReader(new DependencyConfig(1, false, true), executionConfig, new PeASSFolders(TestConstants.CURRENT_FOLDER),
            resultsFolders, new EnvironmentVariables());
      Set<TestCase> tests = reader.getTests(iterator, "", "", new MeasurementConfiguration(1));

      Assert.assertEquals(tests.size(), 0);
   }

   public static void checkVersion(final Dependencies dependencies, final String newestVersion, final int versions) {
      Assert.assertTrue(resultsFolders.getDependencyFile().exists());
      MatcherAssert.assertThat(dependencies.getVersions(), Matchers.aMapWithSize(versions));

      MatcherAssert.assertThat(dependencies.getVersions().get(newestVersion), Matchers.notNullValue());
      final TestSet testSet = getTestset(dependencies, newestVersion);
      Assert.assertEquals(new TestCase("defaultpackage.TestMe#testMe"), testSet.getTests().toArray()[0]);
   }

   private static TestSet getTestset(final Dependencies dependencies, final String newestVersion) {
      final TestSet testSet = dependencies.getVersions().get(newestVersion)
            .getChangedClazzes()
            .get(new ChangedEntity("defaultpackage.NormalDependency", "", ""));
      return testSet;
   }
}
