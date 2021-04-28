package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.ci.ContinuousFolderUtil;


/**
 * Tests ContinuousFolderUtil assuming Peass is still in a git repo
 * @author DaGeRe
 *
 */
public class TestContinuousFolderUtil {
   
   @Test
   public void testSubFolderPath() throws IOException {
      File test = new File("."); 
      String path = ContinuousFolderUtil.getSubFolderPath(test);
      Assert.assertEquals("peass" + File.separator + "analysis", path);
   }
   
   @Test
   public void testFolderPath() throws IOException {
      File test = new File(".."); 
      String path = ContinuousFolderUtil.getSubFolderPath(test);
      Assert.assertEquals("peass", path);
   }
   
   
}
