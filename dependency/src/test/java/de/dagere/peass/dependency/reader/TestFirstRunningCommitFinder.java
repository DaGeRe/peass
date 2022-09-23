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

   @Test
   public void testFirstRunningCommitFinder() {
      List<String> dummyCommits = Arrays.asList(new String[] { "000001", "000002", "000003", "000004" });

      try (MockedStatic<ExecutorCreator> executorCreator = Mockito.mockStatic(ExecutorCreator.class)) {
         executorCreator.when(() -> ExecutorCreator.hasBuildfile(Mockito.any(), Mockito.any())).thenReturn(true);

         TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);
         executorCreator.when(() -> ExecutorCreator.createExecutor(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockedExecutor);
         
         Mockito.when(mockedExecutor.isCommitRunning("000001")).thenReturn(false);
         Mockito.when(mockedExecutor.isCommitRunning("000002")).thenReturn(true);
         
         PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
         FakeCommitIterator iterator = new FakeCommitIterator(TestConstants.CURRENT_FOLDER, dummyCommits);
         FirstRunningCommitFinder commitFinder = new FirstRunningCommitFinder(folders, Mockito.mock(VersionKeeper.class), iterator, new ExecutionConfig(),
               new EnvironmentVariables());

         boolean found = commitFinder.searchFirstRunningCommit();
         Assert.assertTrue(found);
         
         Assert.assertEquals("000002", iterator.getTag());
      }
   }
}
