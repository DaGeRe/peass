package de.dagere.peass.dependency.execution;

import java.io.File;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.execution.ArgLineBuilder;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestArgLineBuilder {

   @Test
   public void testKieker() {
      JUnitTestTransformer mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfiguration config = new MeasurementConfiguration(2);
      config.setUseKieker(true);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);

      ArgLineBuilder builder = new ArgLineBuilder(mockedTransformer, new File("/tmp/asd"));

      String argLineMaven = builder.buildArgline(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineMaven, Matchers.containsString("-javaagent"));
      
      String argLineGradle = builder.buildArglineGradle(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineGradle, Matchers.containsString("-javaagent"));
   }
   
   @Test
   public void testNoKieker() {
      JUnitTestTransformer mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfiguration config = new MeasurementConfiguration(2);
      config.setUseKieker(false);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);

      ArgLineBuilder builder = new ArgLineBuilder(mockedTransformer, new File("/tmp/asd"));

      String argLineMaven = builder.buildArgline(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineMaven, Matchers.not(Matchers.containsString("-javaagent")));
      
      String argLineGradle = builder.buildArglineGradle(new File("/tmp/asd"));
      MatcherAssert.assertThat(argLineGradle, Matchers.not(Matchers.containsString("-javaagent")));
   }

}
