package de.dagere.peass.config;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.utils.Constants;

public class TestSerialization {
   
   @Test
   public void testExecutionConfigDefaultValues() throws JsonProcessingException {
      ExecutionConfig config = new ExecutionConfig();

      String serialized = Constants.OBJECTMAPPER.writeValueAsString(config);
      
      System.out.println(serialized);
      
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("testExecutor")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("testTransformer")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("includes")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("excludes")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("testGoal")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("startcommit")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("endcommit")));
      
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("gradleJavaPluginName")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("gradleSpringBootPluginName")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("properties")));
      
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("clazzFolders")));
      MatcherAssert.assertThat(serialized, Matchers.not(Matchers.containsString("testClazzFolders")));
   }
}
