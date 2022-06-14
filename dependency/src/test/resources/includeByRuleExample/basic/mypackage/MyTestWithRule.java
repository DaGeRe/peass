package mypackage;

import org.junit.Test;

public class MyTestWithRule {
   @ClassRule
   public static DockerRule esRule   = new DockerRule(ImmutableDockerConfig.builder().name("test-es");
   
   @Test
   public void testMe() {
      
   }
}
