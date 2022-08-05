package de.dagere.peass.measurement.kieker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.KiekerResultHandler;
import de.dagere.peass.measurement.dependencyprocessors.OnceRunner;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.vcs.GitUtils;

public class DirectKiekerExecutionIT {

   @BeforeEach
   public void initializeFolders() throws IOException {
      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.deleteDirectory(TestConstants.CURRENT_PEASS);
      FileUtils.copyDirectory(DependencyTestConstants.BASIC_STATE, DependencyTestConstants.CURRENT);
   }

   @Test
   public void testDirectKiekerExecution() {
      MeasurementConfig config = new MeasurementConfig(2);
      config.setIterations(10);
      config.setDirectlyMeasureKieker(true);
      JUnitTestTransformer testTransformer = new JUnitTestTransformer(TestConstants.CURRENT_FOLDER, config);
      PeassFolders folders = new PeassFolders(DependencyTestConstants.CURRENT);
      MavenTestExecutor executor = new MavenTestExecutor(folders, testTransformer, new EnvironmentVariables());

      runOnce(testTransformer, folders, executor);

      File expectedResultFile = new File(folders.getTempMeasurementFolder(), "de.peran.example/example/defaultpackage.TestMe/testMe.json");
      Assert.assertTrue(expectedResultFile.exists());

      JSONDataLoader loader = new JSONDataLoader(expectedResultFile);
      List<DatacollectorResult> collectors = loader.getFullData().getMethods().get(0).getDatacollectorResults();
      MatcherAssert.assertThat(collectors, IsIterableWithSize.iterableWithSize(1));

      runOnce(testTransformer, folders, executor);

      JSONDataLoader loaderSecondRun = new JSONDataLoader(expectedResultFile);
      List<DatacollectorResult> collectorsSecondRun = loaderSecondRun.getFullData().getMethods().get(0).getDatacollectorResults();
      MatcherAssert.assertThat(collectorsSecondRun, IsIterableWithSize.iterableWithSize(2));
   }

   private void runOnce(JUnitTestTransformer testTransformer, PeassFolders folders, MavenTestExecutor executor) {
      try (MockedStatic<GitUtils> gu = Mockito.mockStatic(GitUtils.class)) {
         OnceRunner runner = new OnceRunner(folders, executor, Mockito.mock(ResultOrganizer.class), Mockito.mock(KiekerResultHandler.class));

         testTransformer.determineVersions(Arrays.asList(new File[] { DependencyTestConstants.CURRENT }));
         testTransformer.transformTests();

         runner.runOnce(new TestMethodCall("defaultpackage.TestMe", "testMe"), "123456", 0, folders.getMeasureLogFolder());
      }
   }
}
