package de.dagere.peass.dependency.reader;

import java.util.Arrays;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestDependencies;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependencyprocessors.VersionComparatorInstance;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.vcs.VersionIterator;

public class TestInitialVersionReader {

   private static final TestCase TESTCASE = new TestCase("de.dagere.peass.MyTest#test");
   
   private static final String VERSION2 = "000002";
   private static final String VERSION1 = "000001";

   @Test
   public void testCompletedVersionReading() {
      DependencyManager dependencyManagerMock = Mockito.mock(DependencyManager.class);
      TestDependencies currentTestDependencies = new TestDependencies();
      Mockito.when(dependencyManagerMock.getDependencyMap()).thenReturn(currentTestDependencies);

      TestExecutor testExecutorMock = Mockito.mock(TestExecutor.class);
      Mockito.when(dependencyManagerMock.getExecutor()).thenReturn(testExecutorMock);
      Mockito.when(testExecutorMock.getJDKVersion()).thenReturn(11);

      StaticTestSelection dependencyResult = new StaticTestSelection();
      dependencyResult.getInitialversion().setVersion(VERSION1);
      dependencyResult.getInitialversion().addDependency(TESTCASE, new ChangedEntity("de.dagere.peass.MyCallee#method"));

      VersionStaticSelection staticSelection = new VersionStaticSelection();
      staticSelection.getChangedClazzes().put(new ChangedEntity("de.dagere.peass.MyCallee#method"), new TestSet(TESTCASE));
      dependencyResult.getVersions().put(VERSION2, staticSelection);

      InitialVersionReader reader = new InitialVersionReader(dependencyResult, dependencyManagerMock, Mockito.mock(VersionIterator.class));
      reader.readCompletedVersions(new VersionComparatorInstance(Arrays.asList(new String[] { VERSION1, VERSION2 })));
      
      Set<ChangedEntity> currentlyCalledClasses = currentTestDependencies.getDependencyMap().get(TESTCASE).getCalledClasses();
      MatcherAssert.assertThat(currentlyCalledClasses, 
            IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.MyCallee")));
      MatcherAssert.assertThat(currentlyCalledClasses, 
            IsIterableContaining.hasItem(new ChangedEntity("de.dagere.peass.MyTest")));
      
      
   }

}
