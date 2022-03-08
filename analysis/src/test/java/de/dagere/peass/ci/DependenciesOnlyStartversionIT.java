package de.dagere.peass.ci;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.dagere.peass.TestConstants;
import de.dagere.peass.ci.helper.GitProjectBuilder;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.VersionIteratorGit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DependenciesOnlyStartversionIT {

   private static GitProjectBuilder builder;

   StaticTestSelection dependencies;

   @BeforeEach
   public void cleanDependencies() throws Exception {
      FileUtils.deleteDirectory(ContinuousDependencyReaderIT.resultsFolders.getStaticTestSelectionFile().getParentFile());
      Assert.assertFalse(ContinuousDependencyReaderIT.resultsFolders.getStaticTestSelectionFile().exists());

      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      builder = new GitProjectBuilder(TestConstants.CURRENT_FOLDER, new File("../dependency/src/test/resources/dependencyIT/basic_state"));

      VersionIteratorGit iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);
      iterator.goToFirstCommit();

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setVersion(iterator.getTag());
      executionConfig.setVersionOld(iterator.getPrevious().getTag());
      ContinuousDependencyReader reader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, executionConfig, new KiekerConfig(true), new PeassFolders(TestConstants.CURRENT_FOLDER),
            ContinuousDependencyReaderIT.resultsFolders, new EnvironmentVariables());
      dependencies = reader.getDependencies(iterator, "");

      Assert.assertEquals(0, dependencies.getVersions().size());
   }

   @Order(1)
   @Test
   public void testBasicVersionReading() throws Exception {
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/changed_class"), "test 1");

      VersionIteratorGit iterator = new VersionIteratorGit(TestConstants.CURRENT_FOLDER);
      iterator.goToFirstCommit();
      iterator.goToNextCommit();

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setVersion(iterator.getTag());
      executionConfig.setVersionOld(iterator.getPrevious().getTag());
      ContinuousDependencyReader reader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, executionConfig, new KiekerConfig(true), new PeassFolders(TestConstants.CURRENT_FOLDER),
            ContinuousDependencyReaderIT.resultsFolders, new EnvironmentVariables());
      dependencies = reader.getDependencies(iterator, "");

      final String lastTag = builder.getTags().get(builder.getTags().size() - 1);
      checkVersion(dependencies, lastTag, 1);
   }

   private void checkVersion(final StaticTestSelection dependencies, final String newestVersion, final int versions) {
      Assert.assertTrue(ContinuousDependencyReaderIT.resultsFolders.getStaticTestSelectionFile().exists());
      MatcherAssert.assertThat(dependencies.getVersions(), Matchers.aMapWithSize(versions));

      MatcherAssert.assertThat(dependencies.getVersions().get(newestVersion), Matchers.notNullValue());
      final TestSet testSet = getTestset(dependencies, newestVersion);
      Assert.assertEquals(new TestCase("defaultpackage.TestMe#testMe"), testSet.getTests().toArray()[0]);
   }

   private TestSet getTestset(final StaticTestSelection dependencies, final String newestVersion) {
      final TestSet testSet = dependencies.getVersions().get(newestVersion)
            .getChangedClazzes()
            .get(new ChangedEntity("defaultpackage.NormalDependency", "", ""));
      return testSet;
   }
}
