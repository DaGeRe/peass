package de.dagere.peass.measurement.analysis;

import java.io.File;



import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.DependencyTester;
import de.dagere.peass.measurement.rca.helper.VCSTestUtils;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class TestDependencyTester {

   public static final TestCase EXAMPLE_TESTCASE = new TestCase("de.peass.MyTest", "test");
   
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testFiles() throws Exception {
      
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<ExecutorCreator> mockedExecutor = Mockito.mockStatic(ExecutorCreator.class);
            MockedStatic<GitUtils> utils = Mockito.mockStatic(GitUtils.class)) {
         VCSTestUtils.mockGetVCS(mockedVCS);
         
         final PeassFolders folders = new PeassFolders(folder.getRoot());
         final MeasurementConfig configuration = new MeasurementConfig(4, "2", "1");
         
         VCSTestUtils.mockExecutor(mockedExecutor, folders, configuration);

         final DependencyTester tester = new DependencyTester(folders, configuration, new EnvironmentVariables());
         
         tester.evaluate(EXAMPLE_TESTCASE);

         checkResult(folders);
      }
      
   }

   public static void checkResult(final PeassFolders folders)  {
      final File expectedSummaryResultFile = folders.getSummaryFile(EXAMPLE_TESTCASE);
      Assert.assertTrue(expectedSummaryResultFile + " should exist", expectedSummaryResultFile.exists());

      final Kopemedata data = JSONDataLoader.loadData(expectedSummaryResultFile);
      final DatacollectorResult collector = data.getFirstMethodResult().getDatacollectorResults().get(0);
      final VMResultChunk chunk = collector.getChunks().get(0);
      Assert.assertEquals(105, chunk.getResults().get(0).getValue(), 0.1);
      Assert.assertEquals(5, chunk.getResults().get(0).getRepetitions());
      Assert.assertEquals(11, chunk.getResults().get(0).getIterations());
      Assert.assertEquals(10, chunk.getResults().get(0).getWarmup());
   }
   
}
