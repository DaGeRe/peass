package de.dagere.peass.dependency.reader;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependencytests.helper.FakeCommitIterator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;

public class TestFirstRunningCommitFinder {
   
   private static final List<String> DUMMY_COMMITS = Arrays.asList(new String[] { "000001", "000002", "000003", "000004" });

   @Test
   public void testFirstRunningCommitFinder() {
      try (MockedStatic<ExecutorCreator> executorCreator = Mockito.mockStatic(ExecutorCreator.class)) {
         TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);
         
         Mockito.when(mockedExecutor.isCommitRunning("000001")).thenReturn(false);
         Mockito.when(mockedExecutor.isCommitRunning("000002")).thenReturn(true);
         
         FakeCommitIterator iterator = new FakeCommitIterator(TestConstants.CURRENT_FOLDER, DUMMY_COMMITS);
         
         FirstRunningCommitFinder commitFinder = createFinder(executorCreator, mockedExecutor, iterator);

         boolean found = commitFinder.searchFirstRunningCommit();
         Assert.assertTrue(found);
         
         Assert.assertEquals("000002", iterator.getTag());
      }
   }
   
   @Test
   public void testLatestRunningCommitFinder() {
      try (MockedStatic<ExecutorCreator> executorCreator = Mockito.mockStatic(ExecutorCreator.class)) {
         TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);
         
         Mockito.when(mockedExecutor.isCommitRunning("000001")).thenReturn(true);
         Mockito.when(mockedExecutor.isCommitRunning("000002")).thenReturn(false);
         Mockito.when(mockedExecutor.isCommitRunning("000003")).thenReturn(false);
         
         FakeCommitIterator iterator = new FakeCommitIterator(TestConstants.CURRENT_FOLDER, DUMMY_COMMITS);
         iterator.goToNamedCommit("000003");
         
         FirstRunningCommitFinder commitFinder = createFinder(executorCreator, mockedExecutor, iterator);

         boolean found = commitFinder.searchLatestRunningCommit();
         Assert.assertTrue(found);
         
         Assert.assertEquals("000001", iterator.getTag());
      }
   }
   
   @Test
   public void testLatestRunningCommitFinderSecondCommit() {
      try (MockedStatic<ExecutorCreator> executorCreator = Mockito.mockStatic(ExecutorCreator.class)) {
         TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);
         
         Mockito.when(mockedExecutor.isCommitRunning("000001")).thenReturn(true);
         Mockito.when(mockedExecutor.isCommitRunning("000002")).thenReturn(true);
         Mockito.when(mockedExecutor.isCommitRunning("000003")).thenReturn(false);
         
         FakeCommitIterator iterator = new FakeCommitIterator(TestConstants.CURRENT_FOLDER, DUMMY_COMMITS);
         iterator.goToNamedCommit("000003");
         
         FirstRunningCommitFinder commitFinder = createFinder(executorCreator, mockedExecutor, iterator);

         boolean found = commitFinder.searchLatestRunningCommit();
         Assert.assertTrue(found);
         
         Assert.assertEquals("000002", iterator.getTag());
      }
   }

   private FirstRunningCommitFinder createFinder(MockedStatic<ExecutorCreator> executorCreator, TestExecutor mockedExecutor, FakeCommitIterator iterator) {
      executorCreator.when(() -> ExecutorCreator.hasBuildfile(Mockito.any(), Mockito.any())).thenReturn(true);
      executorCreator.when(() -> ExecutorCreator.createExecutor(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockedExecutor);
      
      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      
      FirstRunningCommitFinder commitFinder = new FirstRunningCommitFinder(folders, Mockito.mock(CommitKeeper.class), iterator, new ExecutionConfig(),
            new EnvironmentVariables());
      return commitFinder;
   }
}
