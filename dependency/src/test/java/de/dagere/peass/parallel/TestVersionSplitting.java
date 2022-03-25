package de.dagere.peass.parallel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.parallel.OneReader;
import de.dagere.peass.dependency.parallel.PartialDependenciesMerger;
import de.dagere.peass.dependency.persistence.InitialVersion;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.FirstRunningVersionFinder;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.dependencytests.helper.FakeVersionIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.VersionIterator;

public class TestVersionSplitting {

   @BeforeEach
   public void before() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.deleteDirectory(TestConstants.CURRENT_PEASS);
      TestConstants.CURRENT_FOLDER.mkdir();
   }
   

   static class DummyReader extends DependencyReader {

      public DummyReader(final File dummyFolder, final VersionIterator iterator, final ChangeManager manager) throws IOException {
         super(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(dummyFolder), null, null, iterator, manager, new ExecutionConfig(1), new KiekerConfig(true), new EnvironmentVariables());
      }

      @Override
      public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException {
         dependencyManager = new DependencyManager(folders, new ExecutionConfig(60), new KiekerConfig(true), new EnvironmentVariables());
         dependencyResult.setInitialversion(new InitialVersion());
         dependencyResult.getInitialversion().setVersion(iterator.getTag());
         return true;
      }

      static Set<String> nonRunning = new HashSet<>(Arrays.asList("4", "5"));

      @Override
      public void readVersion() throws IOException, FileNotFoundException {
         System.out.println(nonRunning + " " + iterator.getTag() + " " + nonRunning.contains(iterator.getTag()));
         if (!nonRunning.contains(iterator.getTag())) {
            dependencyResult.getVersions().put(iterator.getTag(), null);
            System.out.println("Reading: " + iterator.getTag());
         }
      }
   }

   @Test
   public void testSplittingNonRunning() throws IOException {
      List<GitCommit> commits = ParallelTestUtil.getCommits();
      int count = 3;
      int size = commits.size() > 2 * count ? commits.size() / count : 2;

      for (int i = 1; i < 10; i++) {
         for (int j = i + 1; j < 10; j++) {
            DummyReader.nonRunning = new HashSet<>(Arrays.asList(String.valueOf(i), String.valueOf(j)));

            List<StaticTestSelection> dependencies = new LinkedList<>();
            for (int chunk = 0; chunk < count; chunk++) {
               final int max = Math.min((chunk + 1) * size + 3, commits.size());// Assuming one in three commits should contain a source-change
               readUntilMax(commits, dependencies, chunk, chunk * size, max);
            }

            StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(dependencies);

            System.out.println(merged.getVersions().keySet() + " " + merged.getVersions().size());
            Assert.assertEquals("Error in " + DummyReader.nonRunning, 7, merged.getVersions().size());
         }
      }
   }
   
   @Test
   public void testEmptyMerging() {
      StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(new LinkedList<>());
      Assert.assertEquals(0, merged.getVersions().size());
   }

   @Test
   public void testSplitting() throws IOException {
      List<GitCommit> commits = ParallelTestUtil.getCommits();

      int count = 3;
      int size = commits.size() > 2 * count ? commits.size() / count : 2;

      List<StaticTestSelection> dependencies = new LinkedList<>();
      for (int chunk = 0; chunk < count; chunk++) {
         final int max = Math.min((chunk + 1) * size + 3, commits.size());// Assuming one in three commits should contain a source-change
         readUntilMax(commits, dependencies, chunk, chunk * size, max);
      }

      StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(dependencies);

      System.out.println(merged.getVersions().keySet());
      Assert.assertEquals(7, merged.getVersions().size());
   }

   @Test
   public void testSplittingStrangeDistribution() throws IOException {
      List<GitCommit> commits = ParallelTestUtil.getCommits();

      List<StaticTestSelection> dependencies = new LinkedList<>();
      readUntilMax(commits, dependencies, 0, 0, 6);
      readUntilMax(commits, dependencies, 1, 6, 8);
      readUntilMax(commits, dependencies, 2, 7, 10);

      StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(dependencies);

      System.out.println(merged.getVersions().keySet());
      Assert.assertEquals(7, merged.getVersions().size());
   }

   private void readUntilMax(final List<GitCommit> commits, final List<StaticTestSelection> dependencies, final int i, final int min, final int max) throws IOException {
      final List<GitCommit> currentCommits = commits.subList(min, max);
      final List<GitCommit> reserveCommits = commits.subList(max - 1, commits.size());
      final GitCommit minimumCommit = commits.get(Math.min(max, commits.size() - 1));
      System.out.println("Minimum: " + minimumCommit.getTag());
      readDummyDependencies(dependencies, i, currentCommits, reserveCommits, minimumCommit);
   }

   private void readDummyDependencies(final List<StaticTestSelection> dependencies, final int i, final List<GitCommit> currentCommits, final List<GitCommit> reserveCommits,
         final GitCommit minimumCommit)
         throws IOException {
      File dummyFolder = new File(TestConstants.CURRENT_FOLDER, "part_" + i);
      dummyFolder.mkdir();
      File pom = new File(dummyFolder, "pom.xml");
      try (BufferedWriter newBufferedWriter = Files.newBufferedWriter(pom.toPath())) {
         newBufferedWriter.write("<project></project>");
      }

      final VersionIterator fakeIterator = new FakeVersionIterator(dummyFolder, currentCommits);
      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(null);

      FirstRunningVersionFinder finder = new FirstRunningVersionFinder(null, null, null, null, null) {
         @Override
         public boolean searchFirstRunningCommit() {
            return true;
         }
      };
      
      DummyReader dummy = new DummyReader(dummyFolder, fakeIterator, changeManager);
      System.out.println(minimumCommit.getTag());
      final VersionIterator reserveIterator = new FakeVersionIterator(dummyFolder, reserveCommits);
      OneReader reader = new OneReader(minimumCommit, reserveIterator, dummy, finder);
      reader.run();

      dependencies.add(dummy.getDependencies());
   }

}
