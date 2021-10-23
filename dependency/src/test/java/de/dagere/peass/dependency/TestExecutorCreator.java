package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.GradleTestExecutor;
import de.dagere.peass.dependency.execution.MavenTestExecutor;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.dependency.jmh.JmhTestExecutor;
import de.dagere.peass.dependency.jmh.JmhTestTransformer;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestExecutorCreator {

   private final File temporaryFolder = new File("target/current");

   @BeforeEach
   public void initFolder() throws IOException {
      if (temporaryFolder.exists()) {
         FileUtils.deleteDirectory(temporaryFolder);
      }
      temporaryFolder.mkdirs();
   }

   @Test
   public void testMavenExecutorCreation() throws IOException {
      File pomFile = new File(temporaryFolder, "pom.xml");
      FileUtils.touch(pomFile);

      TestExecutor executor = createExecutor("default", Mockito.mock(JUnitTestTransformer.class));

      MatcherAssert.assertThat(executor, IsInstanceOf.instanceOf(MavenTestExecutor.class));
   }

   @Test
   public void testGradleExecutorCreation() throws IOException {
      File pomFile = new File(temporaryFolder, "build.gradle");
      FileUtils.touch(pomFile);

      TestExecutor executor = createExecutor("default", Mockito.mock(JUnitTestTransformer.class));

      MatcherAssert.assertThat(executor, IsInstanceOf.instanceOf(GradleTestExecutor.class));
   }

   @Test
   public void testJmhExecutorCreation() throws IOException {
      File pomFile = new File(temporaryFolder, "pom.xml");
      FileUtils.touch(pomFile);

      TestExecutor executor = createExecutor(WorkloadType.JMH.getTestExecutor(), Mockito.mock(JmhTestTransformer.class));

      MatcherAssert.assertThat(executor, IsInstanceOf.instanceOf(JmhTestExecutor.class));
   }

   @Test
   public void testOtherExecutorCreation() throws IOException {
      File buildFile = new File(temporaryFolder, "myDummyBuildfile.asd");
      FileUtils.touch(buildFile);

      TestExecutor executor = createExecutor("de.dagere.peass.dependency.DummyExecutor", Mockito.mock(JUnitTestTransformer.class));

      MatcherAssert.assertThat(executor, IsInstanceOf.instanceOf(DummyExecutor.class));
   }

   @Test
   public void testNoKnownCreation() throws IOException {
      File buildFile = new File(temporaryFolder, "myDummyBuildfile.asd");
      FileUtils.touch(buildFile);

      Assertions.assertThrows(RuntimeException.class, () -> {
         TestExecutor executor = createExecutor("de.dagere.peass.dependency.NotExistingExecutor", Mockito.mock(JUnitTestTransformer.class));
      });
   }

   private TestExecutor createExecutor(final String executorName, final TestTransformer transformer) {
      PeassFolders folders = new PeassFolders(temporaryFolder);
      MeasurementConfig config = new MeasurementConfig(2);
      config.getExecutionConfig().setTestExecutor(executorName);
      Mockito.when(transformer.getConfig()).thenReturn(config);

      TestExecutor executor = ExecutorCreator.createExecutor(folders, transformer, new EnvironmentVariables());
      return executor;
   }
}
