package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.dagere.kopeme.generated.Result;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependencyprocessors.AdaptiveTester;
import de.dagere.peass.measurement.MavenTestExecutorMocker;
import de.dagere.peass.measurement.analysis.ResultLoader;
import de.dagere.peass.measurement.rca.helper.VCSTestUtils;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ GitUtils.class, VersionControlSystem.class, ExecutorCreator.class, ResultLoader.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class AdaptiveTesterTest {

   private final TestCase testcase = new TestCase("Dummy#dummyTest");
   private JUnitTestTransformer testGenerator = Mockito.mock(JUnitTestTransformer.class);

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Before
   public void setup() {
      VCSTestUtils.mockGetVCS();
      MavenTestExecutorMocker.mockExecutor();
   }

   @Test
   public void testIterationUpdate() throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      final int vms = 10;
      final MeasurementConfiguration config = new MeasurementConfiguration(vms, "A", "B");
      config.setIterations(1000);

      MeasurementConfiguration config2 = Mockito.spy(config);
      Mockito.when(testGenerator.getConfig()).thenReturn(config2);

      AdaptiveTester tester2 = prepareTester();
      Mockito.doReturn(false).when(tester2).checkIsDecidable(Mockito.eq(testcase), Mockito.anyInt());

      for (int i = 0; i < vms; i++) {
         final Result result1 = new Result();
         result1.setValue(15);
         result1.setIterations(40);
         Mockito.doReturn(result1).when(tester2).getLastResult("A", testcase, i);

         final Result result2 = new Result();
         result2.setValue(17);
         result2.setIterations(40);
         Mockito.doReturn(result2).when(tester2).getLastResult("B", testcase, i);
      }

      tester2.evaluate(testcase);

      Assert.assertEquals(vms, tester2.getFinishedVMs());
      Mockito.verify(config2).setIterations(38);
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
   public void testSkipEarlyDecision() throws IOException, InterruptedException, JAXBException, XmlPullParserException {
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

   private void createEarlyBreakData(final AdaptiveTester tester2) throws JAXBException {
      for (int i = 0; i < 100; i++) {
         final Result result1 = new Result();
         result1.setValue(15);
         result1.setIterations(40);
         Mockito.doReturn(result1).when(tester2).getLastResult("A", testcase, i);

         final Result result2 = new Result();
         result2.setValue(15);
         result2.setIterations(40);
         Mockito.doReturn(result2).when(tester2).getLastResult("B", testcase, i);
      }
   }

   private AdaptiveTester prepareTester() throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      final PeassFolders folders = Mockito.mock(PeassFolders.class);
      Mockito.when(folders.getProjectFolder()).thenReturn(folder.newFolder("test"));
      Mockito.when(folders.getResultFile(Mockito.any(TestCase.class), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
         .thenAnswer((index) -> {
            return new File(folder.getRoot(), "log" + index);
         });
      Mockito.when(folders.getMeasureLogFolder(Mockito.anyString(), Mockito.any(TestCase.class))).thenReturn(folder.newFile("log"));

      AdaptiveTester tester = new AdaptiveTester(folders, testGenerator.getConfig(), new EnvironmentVariables());
      AdaptiveTester tester2 = Mockito.spy(tester);
      Mockito.doNothing().when(tester2).runOneComparison(Mockito.any(File.class), Mockito.any(TestCase.class), Mockito.anyInt());
      return tester2;
   }

   
}
