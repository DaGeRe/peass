package de.dagere.peass.dependency.execution.twiceExecution;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.RTSTestTransformerBuilder;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.reader.twiceExecution.TwiceExecutableChecker;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestTwiceExecutionChecker {

   private static final File PROJECT_FOLDER = new File("target/twiceExecution");

   @Test
   public void testTwiceExecutionFalse() throws IOException {
      FileUtils.deleteDirectory(new File("target/twiceExecution_peass"));
      
      File exampleProject = new File("src/test/resources/testProperties/notExecutableTwice/");
      FileUtils.copyDirectory(exampleProject, PROJECT_FOLDER);

      TestTransformer testTransformer = RTSTestTransformerBuilder.createTestTransformer(new PeassFolders(PROJECT_FOLDER), new ExecutionConfig(), new KiekerConfig());
      TestExecutor executor = ExecutorCreator.createExecutor(new PeassFolders(PROJECT_FOLDER), testTransformer, new EnvironmentVariables());
      TwiceExecutableChecker checker = new TwiceExecutableChecker(executor, new ExecutionData());

      Set<TestMethodCall> tests = new HashSet<>();
      tests.add(new TestMethodCall("defaultpackage.TestMe", "testMe"));
      checker.checkTwiceExecution("000001", tests);
      
      boolean isExecutableTwice = checker.getTestProperties().get(new TestMethodCall("defaultpackage.TestMe", "testMe"));
      Assert.assertFalse(isExecutableTwice);
   }
   
   @Test
   public void testTwiceExecutionTrue() throws IOException {
      FileUtils.deleteDirectory(new File("target/twiceExecution_peass"));
      
      File exampleProject = new File("src/test/resources/dependencyIT/basic_state/");
      FileUtils.copyDirectory(exampleProject, PROJECT_FOLDER);

      TestTransformer testTransformer = RTSTestTransformerBuilder.createTestTransformer(new PeassFolders(PROJECT_FOLDER), new ExecutionConfig(), new KiekerConfig());
      TestExecutor executor = ExecutorCreator.createExecutor(new PeassFolders(PROJECT_FOLDER), testTransformer, new EnvironmentVariables());
      TwiceExecutableChecker checker = new TwiceExecutableChecker(executor, new ExecutionData());

      Set<TestMethodCall> tests = new HashSet<>();
      tests.add(new TestMethodCall("defaultpackage.TestMe", "testMe"));
      checker.checkTwiceExecution("000001", tests);
      
      boolean isExecutableTwice = checker.getTestProperties().get(new TestMethodCall("defaultpackage.TestMe", "testMe"));
      Assert.assertTrue(isExecutableTwice);
   }
}
