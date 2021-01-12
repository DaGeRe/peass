package de.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.measurement.MavenTestExecutorMocker;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VersionControlSystem.class, ExecutorCreator.class })
@PowerMockIgnore("javax.management.*")
public class TestDependencyTester {

   public static final TestCase EXAMPLE_TESTCASE = new TestCase("de.peass.MyTest", "test");
   
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testFiles() throws Exception {
      final PeASSFolders folders = new PeASSFolders(folder.getRoot());
      final MeasurementConfiguration configuration = new MeasurementConfiguration(4, "2", "1");

      MavenTestExecutorMocker.mockExecutor(folders, configuration);

      VCSTestUtils.mockGetVCS();

      final DependencyTester tester = new DependencyTester(folders, configuration);
      
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
