package de.dagere.peass.ci;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitUtils;

public class TestDependencyIteratorBuilder {

   private static final File TEMPORARY_FOLDER = new File("target/temp");
   private static final String SIMPLE_PREDECESSOR = "000001";
   private static final String LAST_RUNNING_VERSION = "00000A";
   private static final String COMMIT_2 = "000002";

   @Test
   public void testRegularIteratorCreation() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(COMMIT_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(SIMPLE_PREDECESSOR);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildCommitDependencies(LAST_RUNNING_VERSION);
         
         CommitIteratorBuilder iterator = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         Assert.assertEquals(COMMIT_2, iterator.getCommit());
         Assert.assertEquals(SIMPLE_PREDECESSOR, iterator.getCommitOld());
      }
   }
   
   @Test
   public void testNightlyBuildWithoutPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(COMMIT_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildCommitDependencies();
         
         CommitIteratorBuilder builder = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         Assert.assertEquals(COMMIT_2, builder.getCommit());
         Assert.assertEquals(null, builder.getCommitOld());
      }
   }
   
   @Test
   public void testNightlyBuildWithoutRunningPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(COMMIT_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildCommitDependencies(SIMPLE_PREDECESSOR);
         dependencies.getCommits().get(SIMPLE_PREDECESSOR).setRunning(false);
         
         CommitIteratorBuilder builder = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         Assert.assertEquals(COMMIT_2, builder.getCommit());
         Assert.assertEquals(SIMPLE_PREDECESSOR, builder.getCommitOld());
      }
   }
   
   @Test
   public void testNightlyBuildWithPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(COMMIT_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildCommitDependencies(SIMPLE_PREDECESSOR, COMMIT_2);
         
         CommitIteratorBuilder builder = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         Assert.assertEquals(COMMIT_2, builder.getCommit());
         Assert.assertNull(builder.getCommitOld());
      }
   }

   @Test
   public void testNightlyBuildIteratorCreation() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(COMMIT_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");
         
         StaticTestSelection dependencies = buildCommitDependencies(LAST_RUNNING_VERSION);

         CommitIteratorBuilder iterator = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         Assert.assertEquals(COMMIT_2, iterator.getCommit());
         Assert.assertEquals(LAST_RUNNING_VERSION, iterator.getCommitOld());
      }
   }

   @Test
   public void testInitialRun() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName("HEAD", TEMPORARY_FOLDER)).thenReturn(COMMIT_2);
         gitUtil.when(() -> GitUtils.getName("HEAD~1", TEMPORARY_FOLDER)).thenReturn(SIMPLE_PREDECESSOR);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         CommitIteratorBuilder iterator = new CommitIteratorBuilder(config, null, new PeassFolders(TEMPORARY_FOLDER));
         Assert.assertEquals(COMMIT_2, iterator.getCommit());
         Assert.assertEquals(SIMPLE_PREDECESSOR, iterator.getCommitOld());
      }
   }
   
   private StaticTestSelection buildCommitDependencies(final String... commitNames) {
      StaticTestSelection dependencies = new StaticTestSelection();
      for (String versionName : commitNames) {
         CommitStaticSelection version = new CommitStaticSelection();
         version.setRunning(true);
         dependencies.getCommits().put(versionName, version);
      }
      
      return dependencies;
   }
}
