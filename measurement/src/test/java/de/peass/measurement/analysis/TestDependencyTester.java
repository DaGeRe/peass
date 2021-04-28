package de.peass.measurement.analysis;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.dependencyprocessors.DependencyTester;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.measurement.MavenTestExecutorMocker;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VersionControlSystem.class, ExecutorCreator.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class TestDependencyTester {

   public static final TestCase EXAMPLE_TESTCASE = new TestCase("de.peass.MyTest", "test");
   
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testFiles() throws Exception {
      VCSTestUtils.mockGetVCS();
      
      final PeASSFolders folders = new PeASSFolders(folder.getRoot());
      final MeasurementConfiguration configuration = new MeasurementConfiguration(4, "2", "1");

      MavenTestExecutorMocker.mockExecutor(folders, configuration);

      final DependencyTester tester = new DependencyTester(folders, configuration, new EnvironmentVariables());
      
      tester.evaluate(EXAMPLE_TESTCASE);

      checkResult(folders);
   }

   public static void checkResult(final PeASSFolders folders) throws JAXBException {
      final File expectedShortresultFile = new File(folders.getFullMeasurementFolder(), EXAMPLE_TESTCASE.getShortClazz() + "_" + EXAMPLE_TESTCASE.getMethod() + ".xml");
      Assert.assertTrue(expectedShortresultFile.exists());

      final Kopemedata data = XMLDataLoader.loadData(expectedShortresultFile);
      final Datacollector collector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      final Chunk chunk = collector.getChunk().get(0);
      Assert.assertEquals(105, chunk.getResult().get(0).getValue(), 0.1);
      Assert.assertEquals(5, chunk.getResult().get(0).getRepetitions());
      Assert.assertEquals(11, chunk.getResult().get(0).getIterations());
      Assert.assertEquals(10, chunk.getResult().get(0).getWarmup());
   }
   
}
