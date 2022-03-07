package de.dagere.peass.vcs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;

public class TestGitUtils {

   private static final String FEATURE_A = "feature-A";
   private static final String PEASS_TEST_MAIN_BRANCH = "peass-test-main";
   private final static File PROJECT_FOLDER = new File(TestConstants.CURRENT_FOLDER, "demo-git");

   private static final File exampleTextFile = new File(PROJECT_FOLDER, "file.txt");

   @BeforeEach
   public void prepareProject() throws InterruptedException, IOException {
      TestUtil.deleteContents(PROJECT_FOLDER);
      PROJECT_FOLDER.mkdirs();
      ProjectBuilderHelper.init(PROJECT_FOLDER);

      FileUtils.writeStringToFile(exampleTextFile, "Dummy", StandardCharsets.UTF_8);
      ProjectBuilderHelper.commit(PROJECT_FOLDER, "Dummy-version for avoiding branch clashes (default branch might be main or master)");

      // Use main branch, regardless of what the system has configured as default branch (e.g. might be master)
      ProjectBuilderHelper.branch(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);
      ProjectBuilderHelper.checkout(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);

      for (int i = 0; i < 3; i++) {
         createCommit(exampleTextFile, "", i);
      }

      ProjectBuilderHelper.branch(PROJECT_FOLDER, FEATURE_A);
      ProjectBuilderHelper.checkout(PROJECT_FOLDER, FEATURE_A);

      for (int i = 0; i < 3; i++) {
         createCommit(exampleTextFile, "A", i);
      }
      ProjectBuilderHelper.checkout(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);
   }

   private void createCommit(final File exampleTextFile, final String prefix, final int i) throws IOException, InterruptedException {
      FileUtils.writeStringToFile(exampleTextFile, prefix + i, StandardCharsets.UTF_8);
      ProjectBuilderHelper.commit(PROJECT_FOLDER, "Version " + prefix + i);
   }

   @Test
   public void testBasicCommitGetting() throws InterruptedException, IOException {
      List<GitCommit> commitsAll = GitUtils.getCommits(PROJECT_FOLDER, true, false, true);
      Assert.assertEquals(commitsAll.size(), 7);

      List<GitCommit> commits = GitUtils.getCommits(PROJECT_FOLDER, false, false, true);
      Assert.assertEquals(commits.size(), 4);

      ProjectBuilderHelper.merge(PROJECT_FOLDER, FEATURE_A);

      List<GitCommit> commitsMerged = GitUtils.getCommits(PROJECT_FOLDER, false, false, true);
      Assert.assertEquals(commitsMerged.size(), 7);

      MatcherAssert.assertThat(commitsMerged.get(0).getComitter(), Matchers.equalTo("Anonym <anonym@generated.org>"));
      Assert.assertNotNull(commitsMerged.get(0).getDate());

      MatcherAssert.assertThat(commitsMerged.get(commitsMerged.size() - 1).getMessage(), Matchers.containsString("Version A2"));
      MatcherAssert.assertThat(commitsMerged.get(0).getMessage(), Matchers.containsString("Dummy-version for avoiding branch clashes"));
   }

   @Test
   public void testMergeCommits() throws InterruptedException, IOException {
      createMergeCommit(exampleTextFile, 3);
      createMergeCommit(exampleTextFile, 4);

      List<GitCommit> commitsRegular = GitUtils.getCommits(PROJECT_FOLDER, false, false, false);
      Assert.assertEquals(commitsRegular.size(), 13);

      // Current linearization produces 7, 8, 10 or 11 commits based on hashes; every of these linearizations is ok (it just should not be 13)
      List<GitCommit> commitsLinear = GitUtils.getCommits(PROJECT_FOLDER, false, true, false);
      MatcherAssert.assertThat(commitsLinear.size(), Matchers.anyOf(Matchers.is(7), Matchers.is(8), Matchers.is(10), Matchers.is(11)));
   }

   private void createMergeCommit(final File exampleTextFile, final int index) throws InterruptedException, IOException {
      createCommit(exampleTextFile, "", index);

      ProjectBuilderHelper.checkout(PROJECT_FOLDER, FEATURE_A);
      createCommit(exampleTextFile, "A", index);

      ProjectBuilderHelper.checkout(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);
      ProjectBuilderHelper.mergeTheirs(PROJECT_FOLDER, FEATURE_A);
   }

   @Test
   public void testFilterList() {
      List<GitCommit> regularExample = buildExampleList();
      GitUtils.filterList("000001", "000003", regularExample);
      Assert.assertEquals(3, regularExample.size());
      
      RuntimeException thrown1 = Assert.assertThrows(RuntimeException.class, () -> {
         List<GitCommit> secondExample = buildExampleList();
         GitUtils.filterList("000005", "000003", secondExample);
      });
      Assert.assertEquals("Startversion 000005 after endversion 000003", thrown1.getMessage());
      
      RuntimeException thrown2 = Assert.assertThrows(RuntimeException.class, () -> {
         List<GitCommit> secondExample = buildExampleList();
         GitUtils.filterList("00000A", "000003", secondExample);
      });
      Assert.assertEquals("Startversion 00000A not found at all, but endversion 000003 found", thrown2.getMessage());
   }

   private List<GitCommit> buildExampleList() {
      List<GitCommit> regularExample = new LinkedList<>();
      regularExample.add(new GitCommit("000001", null, null, null));
      regularExample.add(new GitCommit("000002", null, null, null));
      regularExample.add(new GitCommit("000003", null, null, null));
      regularExample.add(new GitCommit("000004", null, null, null));
      regularExample.add(new GitCommit("000005", null, null, null));
      return regularExample;
   }
}
