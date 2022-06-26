package de.dagere.peass.dependency.execution.maven;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

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
      config.setDirectlyMeasureKieker(true);
      JUnitTestTransformer testTransformer = new JUnitTestTransformer(TestConstants.CURRENT_FOLDER, config);
      PeassFolders folders = new PeassFolders(DependencyTestConstants.CURRENT);
      MavenTestExecutor executor = new MavenTestExecutor(folders, testTransformer, new EnvironmentVariables());

      testTransformer.determineVersions(Arrays.asList(new File[] { DependencyTestConstants.CURRENT }));
      testTransformer.transformTests();

      executor.prepareKoPeMeExecution(new File(folders.getMeasureLogFolder(), "prepare.txt"));
      
      executor.executeTest(new TestCase("defaultpackage.TestMe#testMe"), folders.getMeasureLogFolder(), 60);
      
      File expectedResultFile = new File(folders.getTempMeasurementFolder(), "de.peran.example/example/defaultpackage.TestMe/testMe.json");
      Assert.assertTrue(expectedResultFile.exists());
      
      JSONDataLoader loader = new JSONDataLoader(expectedResultFile);
      List<DatacollectorResult> collectors = loader.getFullData().getMethods().get(0).getDatacollectorResults();
      MatcherAssert.assertThat(collectors, IsIterableWithSize.iterableWithSize(1));
   }
}
