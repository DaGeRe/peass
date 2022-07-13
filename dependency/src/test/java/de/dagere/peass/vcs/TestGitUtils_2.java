package de.dagere.peass.vcs;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class TestGitUtils_2 {

   @Test
   @DisabledOnOs(OS.WINDOWS)
   public void testCheckIsUnlockedWithGitCrypt() throws ZipException {

      final String current = DependencyTestConstants.CURRENT.getAbsolutePath();
      final String gitCryptTestString = current + File.separator + "git-crypt-test";
      TestUtil.deleteContents(new File(gitCryptTestString));

      final File gitCryptTestRepoFile = new File(gitCryptTestString + File.separator + "git-crypt-testRepo");
      final ZipFile file = new ZipFile(new File(TestConstants.TEST_RESOURCES, "git-crypt-test.zip"));
      file.extractAll(current);

      Assert.assertEquals(false, GitUtils.checkIsUnlockedWithGitCrypt(gitCryptTestRepoFile));
      GitUtils.unlockWithGitCrypt(gitCryptTestRepoFile, "../git-crypt-testKey");
      Assert.assertEquals(true, GitUtils.checkIsUnlockedWithGitCrypt(gitCryptTestRepoFile));
   }

}
