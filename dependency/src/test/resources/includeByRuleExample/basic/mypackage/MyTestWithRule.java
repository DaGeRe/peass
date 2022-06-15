package mypackage;

import org.junit.Test;

import de.dagere.peass.testtransformation.ClassRule;
import de.dagere.peass.testtransformation.DockerRule;

public class MyTestWithRule {
   @ClassRule
   public static DockerRule esRule   = new DockerRule(ImmutableDockerConfig.builder().name("test-es"));
   
   @Test
   public void testMe() {
      
   }
}
