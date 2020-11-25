package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.TreeBuilder;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.searcher.CauseSearcher;
import de.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcherTest {

   /**
    * Needs own measurement config for kieker activation
    */
   private MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2, TestConstants.V2, TestConstants.V1);

   {
      measurementConfig.setUseKieker(true);
   }

   public void cleanup() {
      final File folder = new File("target/test_peass/");
      try {
         FileUtils.deleteDirectory(folder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testMeasurement() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final TreeBuilder treeBuilderPredecessor = new TreeBuilder();
      final CallTreeNode root1 = treeBuilderPredecessor.getRoot();
      final CallTreeNode root2 = treeBuilderPredecessor.getRoot();

      final LevelDifferentNodeDeterminer lcs = new LevelDifferentNodeDeterminer(Arrays.asList(new CallTreeNode[] { root1 }),
            Arrays.asList(new CallTreeNode[] { root2 }),
            TestConstants.SIMPLE_CAUSE_CONFIG,
            measurementConfig);
      
      root1.setOtherVersionNode(root2);
      root2.setOtherVersionNode(root1);
      treeBuilderPredecessor.buildMeasurements(treeBuilderPredecessor.getRoot());
      
      lcs.calculateDiffering();
      Assert.assertEquals(3, lcs.getMeasureNextLevelPredecessor().size());
      Assert.assertThat(lcs.getMeasureNextLevelPredecessor(), Matchers.hasItem(treeBuilderPredecessor.getA()));
      Assert.assertThat(lcs.getMeasureNextLevelPredecessor(), Matchers.hasItem(treeBuilderPredecessor.getC()));
      Assert.assertThat(lcs.getMeasureNextLevelPredecessor(), Matchers.hasItem(treeBuilderPredecessor.getConstructor()));

      Assert.assertEquals(0, lcs.getTreeStructureDifferingNodes().size());
   }

   @Test
   public void testCauseSearching()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final TreeBuilder builderPredecessor = new TreeBuilder();
      final CallTreeNode root1 = builderPredecessor.getRoot();
      final CallTreeNode root2 = builderPredecessor.getRoot();

      cleanup();
      final File folder = new File("target/test/");
      folder.mkdir();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);
      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);

      final CauseTester measurer = Mockito.mock(CauseTester.class);
      CauseTesterMockUtil.mockMeasurement(measurer, builderPredecessor);
      final CauseSearcher searcher = new LevelCauseSearcher(treeReader, TestConstants.SIMPLE_CAUSE_CONFIG, measurer, measurementConfig,
            new CauseSearchFolders(folder));

      final Set<ChangedEntity> changes = searcher.search();

      System.out.println(changes);
      Assert.assertEquals(1, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.iterator().next().toString());

      final CauseSearchData data = searcher.getRCAData();
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      final double expectedT = new TTest().t(nodeStatistic.getStatisticsOld(), nodeStatistic.getStatisticsCurrent());
      Assert.assertEquals(expectedT, nodeStatistic.getTvalue(), 0.01);
   }

   @Test
   public void testWarmup()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      cleanup();
      final File folder = new File("target/test/");
      folder.mkdir();

      measurementConfig = new MeasurementConfiguration(3, TestConstants.V2, TestConstants.V1);
      // measurementConfig.setUseKieker(true);
      measurementConfig.setWarmup(3);
      measurementConfig.setIterations(5);

      final TreeBuilder builderPredecessor = new TreeBuilder(measurementConfig);
      final CallTreeNode root1 = builderPredecessor.getRoot();
      final CallTreeNode root2 = builderPredecessor.getRoot();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);
      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);

      final CauseTester measurer = Mockito.mock(CauseTester.class);
      CauseTesterMockUtil.mockMeasurement(measurer, builderPredecessor);
      final CauseSearcher searcher = new LevelCauseSearcher(treeReader, TestConstants.SIMPLE_CAUSE_CONFIG, measurer, measurementConfig,
            new CauseSearchFolders(folder));

      final Set<ChangedEntity> changes = searcher.search();

      System.out.println(changes);
      Assert.assertEquals(1, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.iterator().next().toString());

      final CauseSearchData data = searcher.getRCAData();
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      final double expectedT = new TTest().t(nodeStatistic.getStatisticsOld(), nodeStatistic.getStatisticsCurrent());
      System.out.println(nodeStatistic.getMeanCurrent());
      System.out.println(expectedT + " " + nodeStatistic.getTvalue());
      // Assert.assertEquals(nodeStatistic.getMeanCurrent());
      Assert.assertEquals(expectedT, nodeStatistic.getTvalue(), 0.01);
   }

}
