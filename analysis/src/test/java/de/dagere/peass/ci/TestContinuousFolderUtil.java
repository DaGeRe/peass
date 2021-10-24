package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests ContinuousFolderUtil assuming Peass is still in a git repo
 * @author DaGeRe
 *
 */
public class TestContinuousFolderUtil {
   
   // This is the repo name, assuming that peass has still its folder structure and this test is in the analysis submodule
   private String repo_name = new File(".").getAbsoluteFile().getParentFile().getParentFile().getName();
   
   @Test
   public void testSubFolderPath() throws IOException {
      File test = new File("."); 
      String path = ContinuousFolderUtil.getSubFolderPath(test);
      Assert.assertEquals(repo_name + File.separator + "analysis", path);
   }
   
   @Test
   public void testFolderPath() throws IOException {
      File test = new File(".."); 
      String path = ContinuousFolderUtil.getSubFolderPath(test);
      Assert.assertEquals(repo_name, path);
   }
   
   
}
