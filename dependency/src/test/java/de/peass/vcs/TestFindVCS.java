package de.peass.vcs;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests whether the vcs is identified correctly - assumes that Peass still lies in a git repo and that this test still is in the dependency-module
 * @author DaGeRe
 *
 */
public class TestFindVCS {
   
   @Test
   public void testDirectFolder() {
      File testFile = new File("../");
      VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(testFile);
      Assert.assertEquals(VersionControlSystem.GIT, vcs);
   }
   
   @Test
   public void testSubfolder(){
      File testFile = new File(".");
      VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(testFile);
      Assert.assertEquals(VersionControlSystem.GIT, vcs);
   }
   
   @Test(expected = RuntimeException.class)
   public void testNoVCSFolder() {
      File testFile = new File("../../");
      VersionControlSystem vcs = VersionControlSystem.getVersionControlSystem(testFile);
   }
}
