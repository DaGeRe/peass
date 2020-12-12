package de.peass.measurement;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.MeasurementStrategy;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.measurement.analysis.TestDependencyTester;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VersionControlSystem.class, ExecutorCreator.class })
@PowerMockIgnore("javax.management.*")
public class TestParallelMeasurement {
   
   
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Ignore
   @Test
   public void testFiles() throws IOException, InterruptedException, JAXBException {
      final PeASSFolders folders = new PeASSFolders(folder.getRoot());
      final MeasurementConfiguration configuration = new MeasurementConfiguration(4, "2", "1");
      configuration.setMeasurementStrategy(MeasurementStrategy.PARALLEL);

      final JUnitTestTransformer testTransformer = Mockito.mock(JUnitTestTransformer.class);
      Mockito.when(testTransformer.getConfig()).thenReturn(configuration);

      MavenTestExecutorMocker.mockExecutor(folders);

      VCSTestUtils.mockGetVCS();

      final DependencyTester tester = new DependencyTester(folders, testTransformer);
      
      tester.evaluate(TestDependencyTester.EXAMPLE_TESTCASE);

      TestDependencyTester.checkResult(folders);
   }
}
