package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;



import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dataloading.ResultLoader;
import de.dagere.peass.measurement.dependencyprocessors.AdaptiveTester;
import de.dagere.peass.measurement.rca.helper.VCSTestUtils;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.vcs.VersionControlSystem;

public class AdaptiveTesterTest {

   private final TestCase testcase = new TestCase("Dummy#dummyTest");
   private JUnitTestTransformer testGenerator = Mockito.mock(JUnitTestTransformer.class);

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testIterationUpdate() throws IOException, InterruptedException,  XmlPullParserException {
      try (MockedStatic<VersionControlSystem> mockedVCS = Mockito.mockStatic(VersionControlSystem.class);
            MockedStatic<ExecutorCreator> mockedExecutor = Mockito.mockStatic(ExecutorCreator.class);) {
         VCSTestUtils.mockGetVCS(mockedVCS);
         VCSTestUtils.mockExecutor(mockedExecutor);

         final int vms = 10;
         final MeasurementConfig config = new MeasurementConfig(vms, "A", "B");
         config.setIterations(1000);

         MeasurementConfig config2 = Mockito.spy(config);
         Mockito.when(testGenerator.getConfig()).thenReturn(config2);

         AdaptiveTester tester2 = prepareTester();
         Mockito.doReturn(false).when(tester2).checkIsDecidable(Mockito.eq(testcase), Mockito.anyInt());

         for (int i = 0; i < vms; i++) {
            final VMResult result1 = new VMResult();
            result1.setValue(15);
            result1.setIterations(40);
            Mockito.doReturn(result1).when(tester2).getLastResult("A", testcase, i);

            final VMResult result2 = new VMResult();
            result2.setValue(17);
            result2.setIterations(40);
            Mockito.doReturn(result2).when(tester2).getLastResult("B", testcase, i);
         }

         tester2.evaluate(testcase);

         Assert.assertEquals(vms, tester2.getFinishedVMs());
         Mockito.verify(config2).setIterations(38);
      }

   }

   @Ignore
   @Test
   public void testEarlyDecision() throws Exception {
      ResultLoader loader = Mockito.mock(ResultLoader.class);
      Mockito.when(loader.getStatisticsAfter()).thenReturn(new DescriptiveStatistics(new double[] { 15, 15, 15, 15, 15 }));
      Mockito.when(loader.getStatisticsBefore()).thenReturn(new DescriptiveStatistics(new double[] { 15, 15, 15, 15, 15 }));

      final MeasurementConfig config = new MeasurementConfig(100, "A", "B");
      config.setIterations(1000);

      MeasurementConfig config2 = Mockito.spy(config);
      Mockito.when(testGenerator.getConfig()).thenReturn(config2);

      AdaptiveTester tester2 = prepareTester();

      createEarlyBreakData(tester2);

      tester2.evaluate(testcase);

      Assert.assertEquals(31, tester2.getFinishedVMs());
   }

   @Test
   public void testSkipEarlyDecision() throws IOException, InterruptedException,  XmlPullParserException {
      final MeasurementConfig config = new MeasurementConfig(100, "A", "B");
      config.setIterations(1000);
      config.setEarlyStop(false);

      MeasurementConfig config2 = Mockito.spy(config);
      Mockito.when(testGenerator.getConfig()).thenReturn(config2);

      AdaptiveTester tester2 = prepareTester();

      createEarlyBreakData(tester2);

      tester2.evaluate(testcase);

      Assert.assertEquals(100, tester2.getFinishedVMs());
   }

   private void createEarlyBreakData(final AdaptiveTester tester2)  {
      for (int i = 0; i < 100; i++) {
         final VMResult result1 = new VMResult();
         result1.setValue(15);
         result1.setIterations(40);
         Mockito.doReturn(result1).when(tester2).getLastResult("A", testcase, i);

         final VMResult result2 = new VMResult();
         result2.setValue(15);
         result2.setIterations(40);
         Mockito.doReturn(result2).when(tester2).getLastResult("B", testcase, i);
      }
   }

   private AdaptiveTester prepareTester() throws IOException, InterruptedException,  XmlPullParserException {
      final PeassFolders folders = Mockito.mock(PeassFolders.class);
      Mockito.when(folders.getProjectFolder()).thenReturn(folder.newFolder("test"));
      Mockito.when(folders.getProgressFile()).thenReturn(folder.newFile("progress"));
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
