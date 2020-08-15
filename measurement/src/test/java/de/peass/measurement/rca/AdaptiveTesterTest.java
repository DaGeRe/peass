package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

import de.dagere.kopeme.generated.Result;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.measurement.analysis.ResultLoader;
import de.peass.measurement.rca.helper.VCSTestUtils;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ GitUtils.class, VersionControlSystem.class, ExecutorCreator.class, ResultLoader.class })
@PowerMockIgnore("javax.management.*")
public class AdaptiveTesterTest {

   private final TestCase testcase = new TestCase("Dummy#dummyTest");
   private JUnitTestTransformer testGenerator = Mockito.mock(JUnitTestTransformer.class);

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Before
   public void setup() {
      VCSTestUtils.mockGetVCS();
      mockExecutor();
   }

   @Test
   public void testIterationUpdate() throws IOException, InterruptedException, JAXBException {
      final MeasurementConfiguration config = new MeasurementConfiguration(100, "A", "B");
      config.setIterations(1000);

      MeasurementConfiguration config2 = Mockito.spy(config);
      Mockito.when(testGenerator.getConfig()).thenReturn(config2);

      AdaptiveTester tester2 = prepareTester();
      Mockito.doReturn(false).when(tester2).checkIsDecidable(Mockito.eq(testcase), Mockito.anyInt());

      for (int i = 0; i < 100; i++) {
         final Result result1 = new Result();
         result1.setValue(15);
         result1.setExecutionTimes(40);
         Mockito.doReturn(result1).when(tester2).getLastResult("A", testcase, i);

         final Result result2 = new Result();
         result2.setValue(17);
         result2.setExecutionTimes(40);
         Mockito.doReturn(result2).when(tester2).getLastResult("B", testcase, i);
      }

      tester2.evaluate(testcase);

      Assert.assertEquals(100, tester2.getFinishedVMs());
      Mockito.verify(config2).setIterations(40);
   }

   @Test
   public void testMe() throws Exception {
      ResultLoader loader = Mockito.mock(ResultLoader.class);

      PowerMockito.whenNew(ResultLoader.class).withAnyArguments().thenReturn(loader);
      Mockito.when(loader.getStatisticsAfter()).thenReturn(new DescriptiveStatistics(new double[] { 15, 15, 15, 15, 15 }));
      Mockito.when(loader.getStatisticsBefore()).thenReturn(new DescriptiveStatistics(new double[] { 15, 15, 15, 15, 15 }));

      ResultLoader newLoader = new ResultLoader(null, null, null,  15);
      Assert.assertNotNull(newLoader.getStatisticsAfter());
      System.out.println(newLoader.getStatisticsAfter());

      ResultLoader newLoader2 = new ResultLoader(null, null, null, 15);
      System.out.println(newLoader2.getStatisticsAfter());
   }

   @Ignore
   @Test
   public void testEarlyDecision() throws Exception {
      ResultLoader loader = Mockito.mock(ResultLoader.class);
      PowerMockito.whenNew(ResultLoader.class).withAnyArguments().thenReturn(loader);
      Mockito.when(loader.getStatisticsAfter()).thenReturn(new DescriptiveStatistics(new double[] { 15, 15, 15, 15, 15 }));
      Mockito.when(loader.getStatisticsBefore()).thenReturn(new DescriptiveStatistics(new double[] { 15, 15, 15, 15, 15 }));
      
      final MeasurementConfiguration config = new MeasurementConfiguration(100, "A", "B");
      config.setIterations(1000);

      MeasurementConfiguration config2 = Mockito.spy(config);
      Mockito.when(testGenerator.getConfig()).thenReturn(config2);

      AdaptiveTester tester2 = prepareTester();

      createEarlyBreakData(tester2);

      tester2.evaluate(testcase);

      Assert.assertEquals(31, tester2.getFinishedVMs());
   }
   
   @Test
   public void testSkipEarlyDecision() throws IOException, InterruptedException, JAXBException {
      final MeasurementConfiguration config = new MeasurementConfiguration(100, "A", "B");
      config.setIterations(1000);
      config.setEarlyStop(false);

      MeasurementConfiguration config2 = Mockito.spy(config);
      Mockito.when(testGenerator.getConfig()).thenReturn(config2);

      AdaptiveTester tester2 = prepareTester();

      createEarlyBreakData(tester2);

      tester2.evaluate(testcase);

      Assert.assertEquals(100, tester2.getFinishedVMs());
   }

   private void createEarlyBreakData(AdaptiveTester tester2) throws JAXBException {
      for (int i = 0; i < 100; i++) {
         final Result result1 = new Result();
         result1.setValue(15);
         result1.setExecutionTimes(40);
         Mockito.doReturn(result1).when(tester2).getLastResult("A", testcase, i);

         final Result result2 = new Result();
         result2.setValue(15);
         result2.setExecutionTimes(40);
         Mockito.doReturn(result2).when(tester2).getLastResult("B", testcase, i);
      }
   }

   private AdaptiveTester prepareTester() throws IOException, InterruptedException, JAXBException {
      final PeASSFolders folders = Mockito.mock(PeASSFolders.class);
      Mockito.when(folders.getProjectFolder()).thenReturn(folder.newFolder("test"));

      AdaptiveTester tester = new AdaptiveTester(folders, testGenerator);
      AdaptiveTester tester2 = Mockito.spy(tester);
      Mockito.doNothing().when(tester2).runOneComparison(Mockito.any(File.class), Mockito.any(TestCase.class), Mockito.anyInt());
      return tester2;
   }

   private void mockExecutor() {
      final MavenTestExecutor manager = Mockito.mock(MavenTestExecutor.class);

      PowerMockito.mockStatic(ExecutorCreator.class);
      PowerMockito.doAnswer(new Answer<MavenTestExecutor>() {

         @Override
         public MavenTestExecutor answer(final InvocationOnMock invocation) throws Throwable {
            return manager;
         }
      }).when(ExecutorCreator.class);
      ExecutorCreator.createExecutor(Mockito.any(PeASSFolders.class), Mockito.any(JUnitTestTransformer.class));
   }
}
