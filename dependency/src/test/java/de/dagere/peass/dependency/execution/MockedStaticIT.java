package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.utils.Constants;

public class MockedStaticIT {

   @BeforeEach
   public void prepareFolder() throws IOException {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
      TestUtil.deleteContents(TestConstants.CURRENT_PEASS);
      
      File projectFolder = new File("src/test/resources/executionIT/mockedStatic");
      FileUtils.copyDirectory(projectFolder, TestConstants.CURRENT_FOLDER);

   }
   
   @Test
   public void testStaticExecution() throws IOException {
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      MeasurementConfig config = new MeasurementConfig(2);
      config.setIterations(5);
      JUnitTestTransformer testTransformer = new JUnitTestTransformer(TestConstants.CURRENT_FOLDER, config);
      TestExecutor executor = ExecutorCreator.createExecutor(folders, testTransformer, new EnvironmentVariables());
      
      testTransformer.determineVersions(Arrays.asList(new File[] { TestConstants.CURRENT_FOLDER }));
      testTransformer.transformTests();
      
      TestMethodCall test = new TestMethodCall("de.dagere.peass.ExampleTest", "test");
      executor.executeTest(test, new File("target/"), 5);
      
      File resultingFile = new File(folders.getTempMeasurementFolder(), "de.dagere.peass/mockedStaticExample/de.dagere.peass.ExampleTest/test.json");
      Kopemedata data = Constants.OBJECTMAPPER.readValue(resultingFile, Kopemedata.class);
      Assert.assertFalse(data.getFirstResult().isFailure());
      Assert.assertEquals(5, data.getFirstResult().getIterations());
      //TODO The test should fail (since this normally creates an error), and afterwards, Mockito.clearAllCaches(); should be added to the test to make testing possible
   }
}
