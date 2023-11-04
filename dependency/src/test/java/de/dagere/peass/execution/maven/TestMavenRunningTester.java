package de.dagere.peass.execution.maven;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.execution.util.TestWrapperExecution;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;

public class TestMavenRunningTester {

   private MavenTestExecutor executorMock;
   
   @BeforeEach
   public void clearFolder() {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
      executorMock = Mockito.mock(MavenTestExecutor.class);
      Mockito.when(executorMock.doesBuildfileExist()).thenReturn(true);
   }

   @Test
   public void testWithWrapper() throws IOException {
      MavenRunningTester tester = new MavenRunningTester(new PeassFolders(TestConstants.CURRENT_FOLDER), new MeasurementConfig(2), new EnvironmentVariables(),
            new ProjectModules(TestConstants.CURRENT_FOLDER));

      FileUtils.copyDirectory(TestWrapperExecution.WITH_WRAPPER, TestConstants.CURRENT_FOLDER);

      boolean success = tester.isCommitRunning("1", executorMock);
      Assert.assertTrue(success);
   }

   @Test
   public void testWithoutWrapper() throws IOException {
      MavenRunningTester tester = new MavenRunningTester(new PeassFolders(TestConstants.CURRENT_FOLDER), new MeasurementConfig(2), new EnvironmentVariables(),
            new ProjectModules(TestConstants.CURRENT_FOLDER));

      FileUtils.copyDirectory(TestWrapperExecution.NO_WRAPPER, TestConstants.CURRENT_FOLDER);

      boolean success = tester.isCommitRunning("1", executorMock);
      Assert.assertTrue(success);

   }
}
