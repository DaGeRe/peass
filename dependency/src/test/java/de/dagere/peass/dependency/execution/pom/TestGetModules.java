package de.dagere.peass.dependency.execution.pom;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.ProjectModules;

public class TestGetModules {
   
   @Test
   public void testPeassItself() {
      File pomFile = new File("../pom.xml");
      ProjectModules modules = MavenPomUtil.getModules(pomFile, new ExecutionConfig());
      Assert.assertEquals(3, modules.getModules().size());
   }
   
   @Test
   public void testPeassWithPl() {
      File pomFile = new File("../pom.xml");
      ExecutionConfig config = new ExecutionConfig();
      config.setPl("measurement");
      ProjectModules modules = MavenPomUtil.getModules(pomFile, config);
      Assert.assertEquals(2, modules.getModules().size());
   }
   
   @Test
   public void testOtherSimple() {
      File pomFile = new File("src/test/resources/maven-multimodule-pl-example/basic_state/pom.xml");
      ExecutionConfig config = new ExecutionConfig();
      ProjectModules modules = MavenPomUtil.getModules(pomFile, config);
      Assert.assertEquals(4, modules.getModules().size());
   }
   
   @Test
   public void testWrongPl() {
      Assertions.assertThrows(RuntimeException.class, () -> {
         File pomFile = new File("src/test/resources/maven-multimodule-pl-example/basic_state/pom.xml");
         ExecutionConfig config = new ExecutionConfig();
         config.setPl("inner-module-1");
         ProjectModules modules = MavenPomUtil.getModules(pomFile, config);
      });
   }
   
   @Test
   public void testOtherPl1() {
      File pomFile = new File("src/test/resources/maven-multimodule-pl-example/basic_state/pom.xml");
      ExecutionConfig config = new ExecutionConfig();
      config.setPl("de.peass:inner-module-1");
      ProjectModules modules = MavenPomUtil.getModules(pomFile, config);
      Assert.assertEquals(2, modules.getModules().size());
   }

   @Test
   public void testOtherPl2() {
      File pomFile = new File("src/test/resources/maven-multimodule-pl-example/basic_state/pom.xml");
      ExecutionConfig config = new ExecutionConfig();
      config.setPl("de.peass:inner-module-test2");
      ProjectModules modules = MavenPomUtil.getModules(pomFile, config);
      Assert.assertEquals(3, modules.getModules().size());
   }
   
   @Test
   public void testOtherPlUsing() {
      File pomFile = new File("src/test/resources/maven-multimodule-pl-example/basic_state/pom.xml");
      ExecutionConfig config = new ExecutionConfig();
      config.setPl("using-module");
      ProjectModules modules = MavenPomUtil.getModules(pomFile, config);
      Assert.assertEquals(4, modules.getModules().size());
   }
   
   
}
