package de.dagere.peass.ci;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.vcs.VersionIteratorGit;

public class TestDependencyIteratorBuilder {
   
   @Test
   public void testRegularIteratorCreation() {
      ExecutionConfig config = new ExecutionConfig();
      config.setVersionOld("000001");
      config.setVersion("000002");
      
      VersionIteratorGit iterator = DependencyIteratorBuilder.getIterator(config, "000001", "000002", new PeassFolders(new File("target/temp")));
      Assert.assertEquals(2, iterator.getSize());
      Assert.assertEquals("000002", iterator.getTag());
      Assert.assertEquals("000001", iterator.getPredecessor());
   }
}
