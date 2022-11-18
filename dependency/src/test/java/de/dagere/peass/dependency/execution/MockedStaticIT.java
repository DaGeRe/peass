package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class MockedStaticIT {

   @Test
   public void testStaticExecution() throws IOException {
      File projectFolder = new File("src/test/resources/executionIT/mockedStatic");
      FileUtils.copyDirectory(projectFolder, TestConstants.CURRENT_FOLDER);

      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      JUnitTestTransformer testTransformer = new JUnitTestTransformer(TestConstants.CURRENT_FOLDER, new MeasurementConfig(2));
      TestExecutor executor = ExecutorCreator.createExecutor(folders, testTransformer, new EnvironmentVariables());
      
      testTransformer.determineVersions(Arrays.asList(new File[] { TestConstants.CURRENT_FOLDER }));
      testTransformer.transformTests();
      
      TestMethodCall test = new TestMethodCall("de.dagere.peass.ExampleTest", "test");
      executor.executeTest(test, new File("target/"), 5);
   }
}
