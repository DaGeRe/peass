package de.dagere.peass.dependency;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

public class TestExecutorCreation {
   
   @Test
   public void testMavenExecutorCreation() {
      ExecutionConfig config = new ExecutionConfig();
      
      TestTransformer testTransformer = ExecutorCreator.createTestTransformer(Mockito.mock(PeassFolders.class), config, new KiekerConfig(true));
      Assert.assertNotNull(testTransformer);
   }
   
   @Test
   public void testMavenExecutorCreationWithMeasurementConfig() {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      TestTransformer testTransformer = ExecutorCreator.createTestTransformer(Mockito.mock(PeassFolders.class), measurementConfig.getExecutionConfig(), measurementConfig);
      Assert.assertNotNull(testTransformer);
   }
}
