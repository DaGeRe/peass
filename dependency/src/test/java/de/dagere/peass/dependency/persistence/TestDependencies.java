package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestDependencies {
   
   @Test
   public void testWithVersions() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialversion(new InitialVersion());
      dependencies.getInitialversion().setVersion("0");
      dependencies.getVersions().put("1", new VersionStaticSelection());
      dependencies.getVersions().put("2", new VersionStaticSelection());
      dependencies.getVersions().put("3", new VersionStaticSelection());
      
      String[] versionNames = dependencies.getVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("2", versionNames[2]);
      Assert.assertEquals("3", versionNames[3]);
      
      Assert.assertEquals("3", dependencies.getNewestVersion());
   }
   
   @Test
   public void testOnlyStartversion() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialversion(new InitialVersion());
      dependencies.getInitialversion().setVersion("0");
      
      String[] versionNames = dependencies.getVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("0", dependencies.getNewestVersion());
   }
   
   @Test
   public void testRunningersions() {
      StaticTestSelection dependencies = new StaticTestSelection();
      
      dependencies.setInitialversion(new InitialVersion());
      dependencies.getInitialversion().setVersion("0");
      VersionStaticSelection running1 = new VersionStaticSelection();
      running1.setRunning(true);
      dependencies.getVersions().put("1", running1);
      VersionStaticSelection nonRunning2 = new VersionStaticSelection();
      nonRunning2.setRunning(false);
      dependencies.getVersions().put("2", nonRunning2);
      VersionStaticSelection running3 = new VersionStaticSelection();
      running3.setRunning(true);
      dependencies.getVersions().put("3", running3);
      
      String[] versionNames = dependencies.getRunningVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("3", versionNames[2]);
      
      Assert.assertEquals("3", dependencies.getNewestVersion());
   }
}
