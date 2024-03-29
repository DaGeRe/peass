package de.dagere.peass.vcs;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests whether the vcs is identified correctly - assumes that Peass still lies in a git repo and that this test still is in the dependency-module
 * 
 * @author DaGeRe
 *
 */
public class TestFindVCS {

   @Test
   public void testDirectFolder() throws IOException {
      File testFile = new File("../");
      VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(testFile);
      Assert.assertEquals(VersionControlSystem.GIT, vcs);

      File vcsFolder = VersionControlSystem.findVCSFolder(testFile);
      Assert.assertEquals(new File("..").getCanonicalPath(), vcsFolder.getPath());
   }

   @Test
   public void testSubfolder() throws IOException {
      File testFile = new File(".");
      VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(testFile);
      Assert.assertEquals(VersionControlSystem.GIT, vcs);

      File vcsFolder = VersionControlSystem.findVCSFolder(testFile);
      Assert.assertEquals(new File("..").getCanonicalPath(), vcsFolder.getPath());
   }

   @Test
   public void testNoVCSFolder() {
      RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
         File testFile = new File(System.getProperty("java.io.tmpdir"));
         VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(testFile);
      });
      
   }
}
