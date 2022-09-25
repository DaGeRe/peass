package de.dagere.peass.dependency.reader;

import java.util.Arrays;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestDependencies;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.vcs.CommitIterator;

public class TestInitialCommitReader {

   private static final TestMethodCall TESTCASE = new TestMethodCall("de.dagere.peass.MyTest", "test");

   private static final String COMMIT2 = "000002";
   private static final String COMMIT1 = "000001";

   @Test
   public void testCompletedCommitReading() {
      DependencyManager dependencyManagerMock = Mockito.mock(DependencyManager.class);
      TestDependencies currentTestDependencies = new TestDependencies();
      Mockito.when(dependencyManagerMock.getDependencyMap()).thenReturn(currentTestDependencies);

      TestExecutor testExecutorMock = Mockito.mock(TestExecutor.class);
      Mockito.when(dependencyManagerMock.getExecutor()).thenReturn(testExecutorMock);
      Mockito.when(testExecutorMock.getJDKVersion()).thenReturn(11);

      StaticTestSelection dependencyResult = new StaticTestSelection();
      dependencyResult.getInitialcommit().setCommit(COMMIT1);
      dependencyResult.getInitialcommit().addDependency(TESTCASE, new ChangedEntity("de.dagere.peass.MyCallee#method"));

      CommitStaticSelection staticSelection = new CommitStaticSelection();
      staticSelection.getChangedClazzes().put(new ChangedEntity("de.dagere.peass.MyCallee#method"), new TestSet(TESTCASE));
      dependencyResult.getCommits().put(COMMIT2, staticSelection);

      InitialCommitReader reader = new InitialCommitReader(dependencyResult, dependencyManagerMock, Mockito.mock(CommitIterator.class));
      reader.readCompletedCommits(new CommitComparatorInstance(Arrays.asList(new String[] { COMMIT1, COMMIT2 })));

      Set<ChangedEntity> currentlyCalledClasses = currentTestDependencies.getDependencyMap().get(TESTCASE).getCalledClasses();
      MatcherAssert.assertThat(currentlyCalledClasses,
            IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.MyCallee")));
      MatcherAssert.assertThat(currentlyCalledClasses,
            IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.MyTest")));

   }

}
