package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.dagere.peass.utils.StreamGobbler;

public class TestGitCommitBuilder {
   
   private static final Logger LOG = LogManager.getLogger(TestGitCommitBuilder.class);

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testBuilding() throws InterruptedException, IOException {
      LOG.debug("Starting TestGitCommitBuilder");
      File tempFolder = folder.newFolder("testproject");

      GitProjectBuilder builder = new GitProjectBuilder(tempFolder, new File("src/test/resources/peass-demo/version1"));
      LOG.debug("Using temporary folder: {}", tempFolder.getAbsolutePath());

      testLogContains(tempFolder, "Initial Commit");

      builder.addCommit(new File("src/test/resources/peass-demo/version2"), "Slower Version");

      testLogContains(tempFolder, "Initial Commit", "Slower Version");
      
      Assert.assertEquals(2, builder.getTags().size());
      MatcherAssert.assertThat(builder.getTags().get(0), Matchers.not(Matchers.is(builder.getTags().get(1))));
   }

   private void testLogContains(final File tempFolder, final String... commitMessages) throws IOException {
      final Process logProcess = Runtime.getRuntime().exec("git log", new String[0], tempFolder);
      String logOutput = StreamGobbler.getFullProcess(logProcess, false);
      LOG.debug("Output: {}", logOutput);
      MatcherAssert.assertThat(logOutput, Matchers.containsString("commit "));

      for (String commitMessage : commitMessages) {
         MatcherAssert.assertThat(logOutput, Matchers.containsString(commitMessage));
      }
   }
}
