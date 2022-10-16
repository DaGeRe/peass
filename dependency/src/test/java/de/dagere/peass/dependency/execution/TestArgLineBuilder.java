package de.dagere.peass.dependency.execution;

import java.io.File;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestArgLineBuilder {

   @Test
   public void testKieker() {
      JUnitTestTransformer mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.getKiekerConfig().setUseKieker(true);
      config.getKiekerConfig().setUseSourceInstrumentation(false);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);

      ArgLineBuilder builder = new ArgLineBuilder(mockedTransformer, new File("/tmp/asd"));

      String argLineMaven = builder.buildArglineMaven(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineMaven, Matchers.containsString("-javaagent"));
      
      String argLineGradle = builder.buildSystemPropertiesGradle(new File("/tmp/asd"));
//      MatcherAssert.assertThat(argLineGradle, Matchers.containsString("-javaagent"));
      MatcherAssert.assertThat(argLineGradle, Matchers.containsString("kieker.monitoring.configuration"));
   }
   
   @Test
   public void testNoKieker() {
      JUnitTestTransformer mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.getKiekerConfig().setUseKieker(false);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);

      ArgLineBuilder builder = new ArgLineBuilder(mockedTransformer, new File("/tmp/asd"));

      String argLineMaven = builder.buildArglineMaven(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineMaven, Matchers.not(Matchers.containsString("-javaagent")));
      
      String argLineGradle = builder.buildSystemPropertiesGradle(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineGradle, Matchers.not(Matchers.containsString("-javaagent")));
   }
   
   @Test
   public void testAggregatedWriter() {
      JUnitTestTransformer mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.getKiekerConfig().setUseKieker(true);
      config.getKiekerConfig().setUseSourceInstrumentation(true);
      config.getKiekerConfig().setEnableAdaptiveMonitoring(true);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);
      
      ArgLineBuilder builder = new ArgLineBuilder(mockedTransformer, new File("/tmp/asd"));
      
      String argLineMaven = builder.buildArglineMaven(new File("/tmp/asd"));
      System.out.println(argLineMaven);
      MatcherAssert.assertThat(argLineMaven, Matchers.not(Matchers.containsString("  ")));
   }

}
