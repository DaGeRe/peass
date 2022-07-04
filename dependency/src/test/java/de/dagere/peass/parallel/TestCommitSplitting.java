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
import de.dagere.peass.dependency.persistence.InitialCommit;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.FirstRunningVersionFinder;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.dependencytests.helper.FakeCommitIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIterator;

public class TestCommitSplitting {

   @BeforeEach
   public void before() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.deleteDirectory(TestConstants.CURRENT_PEASS);
      TestConstants.CURRENT_FOLDER.mkdir();
   }
   

   static class DummyReader extends DependencyReader {

      public DummyReader(final File dummyFolder, final CommitIterator iterator, final ChangeManager manager) throws IOException {
         super(DependencyTestConstants.DEFAULT_CONFIG_NO_VIEWS, new PeassFolders(dummyFolder), null, null, iterator, manager, new ExecutionConfig(1), new KiekerConfig(true), new EnvironmentVariables());
      }

      @Override
      public boolean readInitialCommit() throws IOException, InterruptedException, XmlPullParserException {
         dependencyManager = new DependencyManager(folders, new ExecutionConfig(60), new KiekerConfig(true), new EnvironmentVariables());
         dependencyResult.setInitialcommit(new InitialCommit());
         dependencyResult.getInitialcommit().setCommit(iterator.getTag());
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
      CommitComparatorInstance comparator = ParallelTestUtil.getCommits();
      int count = 3;
      int size = comparator.getCommits().size() > 2 * count ? comparator.getCommits().size() / count : 2;

      for (int i = 1; i < 10; i++) {
         for (int j = i + 1; j < 10; j++) {
            DummyReader.nonRunning = new HashSet<>(Arrays.asList(String.valueOf(i), String.valueOf(j)));

            List<StaticTestSelection> dependencies = new LinkedList<>();
            for (int chunk = 0; chunk < count; chunk++) {
               final int max = Math.min((chunk + 1) * size + 3, comparator.getCommits().size());// Assuming one in three commits should contain a source-change
               readUntilMax(comparator.getCommits(), dependencies, chunk, chunk * size, max);
            }

            StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(dependencies, comparator);

            System.out.println(merged.getVersions().keySet() + " " + merged.getVersions().size());
            Assert.assertEquals("Error in " + DummyReader.nonRunning, 7, merged.getVersions().size());
         }
      }
   }
   
   @Test
   public void testEmptyMerging() {
      StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(new LinkedList<>(), new CommitComparatorInstance(new LinkedList<>()));
      Assert.assertEquals(0, merged.getVersions().size());
   }

   @Test
   public void testSplitting() throws IOException {
      CommitComparatorInstance comparator = ParallelTestUtil.getCommits();

      int count = 3;
      int size = comparator.getCommits().size() > 2 * count ? comparator.getCommits().size() / count : 2;

      List<StaticTestSelection> dependencies = new LinkedList<>();
      for (int chunk = 0; chunk < count; chunk++) {
         final int max = Math.min((chunk + 1) * size + 3, comparator.getCommits().size());// Assuming one in three commits should contain a source-change
         readUntilMax(comparator.getCommits(), dependencies, chunk, chunk * size, max);
      }

      StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(dependencies, comparator);

      System.out.println(merged.getVersions().keySet());
      Assert.assertEquals(7, merged.getVersions().size());
   }

   @Test
   public void testSplittingStrangeDistribution() throws IOException {
      CommitComparatorInstance comparator  = ParallelTestUtil.getCommits();

      List<StaticTestSelection> dependencies = new LinkedList<>();
      readUntilMax(comparator.getCommits(), dependencies, 0, 0, 6);
      readUntilMax(comparator.getCommits(), dependencies, 1, 6, 8);
      readUntilMax(comparator.getCommits(), dependencies, 2, 7, 10);

      StaticTestSelection merged = PartialDependenciesMerger.mergeDependencies(dependencies, comparator);

      System.out.println(merged.getVersions().keySet());
      Assert.assertEquals(7, merged.getVersions().size());
   }

   private void readUntilMax(final List<String> commits, final List<StaticTestSelection> dependencies, final int i, final int min, final int max) throws IOException {
      final List<String> currentCommits = commits.subList(min, max);
      final List<String> reserveCommits = commits.subList(max - 1, commits.size());
      final String minimumCommit = commits.get(Math.min(max, commits.size() - 1));
      System.out.println("Minimum: " + minimumCommit);
      readDummyDependencies(dependencies, i, currentCommits, reserveCommits, minimumCommit);
   }

   private void readDummyDependencies(final List<StaticTestSelection> dependencies, final int i, final List<String> currentCommits, final List<String> reserveCommits,
         final String minimumCommit)
         throws IOException {
      File dummyFolder = new File(TestConstants.CURRENT_FOLDER, "part_" + i);
      dummyFolder.mkdir();
      File pom = new File(dummyFolder, "pom.xml");
      try (BufferedWriter newBufferedWriter = Files.newBufferedWriter(pom.toPath())) {
         newBufferedWriter.write("<project></project>");
      }

      final CommitIterator fakeIterator = new FakeCommitIterator(dummyFolder, currentCommits);
      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(null);

      FirstRunningVersionFinder finder = new FirstRunningVersionFinder(null, null, null, null, null) {
         @Override
         public boolean searchFirstRunningCommit() {
            return true;
         }
      };
      
      DummyReader dummy = new DummyReader(dummyFolder, fakeIterator, changeManager);
      System.out.println(minimumCommit);
      final CommitIterator reserveIterator = new FakeCommitIterator(dummyFolder, reserveCommits);
      OneReader reader = new OneReader(minimumCommit, reserveIterator, dummy, finder, ParallelTestUtil.getCommits());
      reader.run();

      dependencies.add(dummy.getDependencies());
   }

}
