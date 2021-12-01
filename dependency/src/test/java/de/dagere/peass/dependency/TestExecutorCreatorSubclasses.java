package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestExecutorCreatorSubclasses {
   
   @Test
   public void testSubsubclass() {
      TestTransformer transformer = Mockito.mock(TestTransformer.class);
      MeasurementConfig value = new MeasurementConfig(2);
      value.getExecutionConfig().setTestExecutor("de.dagere.peass.dependency.IndirectSubclass");
      Mockito.when(transformer.getConfig()).thenReturn(value);
      
      TestExecutor executor = ExecutorCreator.createExecutor(null, transformer, new EnvironmentVariables());
      Assert.assertNotNull(executor);
   }
}

class DirectSubclass extends TestExecutor{

   public DirectSubclass(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException, XmlPullParserException {
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
   }

   @Override
   public boolean doesBuildfileExist() {
      return false;
   }

   @Override
   public boolean isVersionRunning(final String version) {
      return false;
   }

   @Override
   public ProjectModules getModules() {
      return null;
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
   }
   
}

class IndirectSubclass extends DirectSubclass{
   public IndirectSubclass(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }
}
