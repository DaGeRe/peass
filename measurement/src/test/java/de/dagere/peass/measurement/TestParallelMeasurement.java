package de.dagere.peass.measurement;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.analysis.TestDependencyTester;
import de.dagere.peass.measurement.dependencyprocessors.DependencyTester;
import de.dagere.peass.measurement.dependencyprocessors.ParallelExecutionRunnable;
import de.dagere.peass.measurement.rca.helper.VCSTestUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class TestParallelMeasurement {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testFiles() throws Exception {
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<ExecutorCreator> mockedExecutor = Mockito.mockStatic(ExecutorCreator.class)) {
         VCSTestUtils.mockGetVCS(mockedVCS);

         final PeassFolders folders = new PeassFolders(folder.getRoot());
         final MeasurementConfig configuration = new MeasurementConfig(4, "2", "1");
         configuration.setMeasurementStrategy(MeasurementStrategy.PARALLEL);
         
         MavenTestExecutorMocker.mockExecutor(mockedExecutor, folders, configuration);
         
         DependencyTester spiedTester = createTesterNoThreads(folders, configuration);
         
         spiedTester.evaluate(TestDependencyTester.EXAMPLE_TESTCASE);

         TestDependencyTester.checkResult(folders);
      }
   }

   /**
    * Creates a tester that does not use Threads; this is necessary since mockito inline does not allow static mocks in Threads
    * @param folders
    * @param configuration
    * @return
    * @throws IOException
    * @throws InterruptedException
    */
   private DependencyTester createTesterNoThreads(final PeassFolders folders, final MeasurementConfig configuration) throws IOException, InterruptedException {
      final DependencyTester tester = new DependencyTester(folders, configuration, new EnvironmentVariables());
      DependencyTester spiedTester = Mockito.spy(tester);
      
      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            ParallelExecutionRunnable[] runnables = invocation.getArgument(0);
            for (ParallelExecutionRunnable runnable : runnables) {
               runnable.run();
            }
            return null;
         }
      }).when(spiedTester).runParallel(Mockito.any());
      return spiedTester;
   }
}
