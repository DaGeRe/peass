package de.dagere.peass.dependency;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.InitialVersion;
import de.dagere.peass.dependency.persistence.Version;

public class TestDependencies {
   
   @Test
   public void testWithVersions() {
      Dependencies dependencies = new Dependencies();
      
      dependencies.setInitialversion(new InitialVersion());
      dependencies.getInitialversion().setVersion("0");
      dependencies.getVersions().put("1", new Version());
      dependencies.getVersions().put("2", new Version());
      dependencies.getVersions().put("3", new Version());
      
      String[] versionNames = dependencies.getVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("1", versionNames[1]);
      Assert.assertEquals("2", versionNames[2]);
      Assert.assertEquals("3", versionNames[3]);
      
      Assert.assertEquals("3", dependencies.getNewestVersion());
   }
   
   @Test
   public void testOnlyStartversion() {
      Dependencies dependencies = new Dependencies();
      
      dependencies.setInitialversion(new InitialVersion());
      dependencies.getInitialversion().setVersion("0");
      
      String[] versionNames = dependencies.getVersionNames();
      
      Assert.assertEquals("0", versionNames[0]);
      Assert.assertEquals("0", dependencies.getNewestVersion());
   }
}
