package de.dagere.peass.measurement.rca.strategytest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.CauseTesterMockUtil;
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.helper.TestConstants;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.dagere.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class LevelCauseSearcherTest {

   /**
    * Needs own measurement config for kieker activation
    */
   private MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2, TestConstants.V2, TestConstants.V1);
   private final File folder = new File("target/test_peass/");
   {
      measurementConfig.setUseKieker(true);
   }

   //Environment
   private TreeBuilder builderPredecessor = new TreeBuilder();
   private CallTreeNode root1;
   private CallTreeNode root2;
   private BothTreeReader treeReader;
   
   //Results
   Set<ChangedEntity> changes;
   CauseSearchData data;

   @Before
   public void cleanup() {
      try {
         FileUtils.deleteDirectory(folder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      folder.mkdir();
   }

   public void buildRoots() {
      root1 = builderPredecessor.getRoot();
      root2 = builderPredecessor.getRoot();

      treeReader = Mockito.mock(BothTreeReader.class);
      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);
   }

   @Test
   public void testMeasurement() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      buildRoots();

      final LevelDifferentNodeDeterminer lcs = new LevelDifferentNodeDeterminer(Arrays.asList(new CallTreeNode[] { root1 }),
            Arrays.asList(new CallTreeNode[] { root2 }),
            TestConstants.SIMPLE_CAUSE_CONFIG,
            measurementConfig);

      root1.setOtherVersionNode(root2);
      root2.setOtherVersionNode(root1);
      builderPredecessor.buildMeasurements(builderPredecessor.getRoot());

      lcs.calculateDiffering();
      Assert.assertEquals(1, lcs.getLevelDifferentCurrent().size());
      Assert.assertEquals(1, lcs.getLevelDifferentPredecessor().size());
//      Assert.assertEquals(3, lcs.getMeasureNextLevelPredecessor().size());
//      Assert.assertThat(lcs.getMeasureNextLevelPredecessor(), Matchers.hasItem(builderPredecessor.getA()));
//      Assert.assertThat(lcs.getMeasureNextLevelPredecessor(), Matchers.hasItem(builderPredecessor.getC()));
//      Assert.assertThat(lcs.getMeasureNextLevelPredecessor(), Matchers.hasItem(builderPredecessor.getConstructor()));

      Assert.assertEquals(0, lcs.getTreeStructureDifferingNodes().size());
   }
   
   @Test
   public void testCauseSearching()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      buildRoots();

      searchChanges(TestConstants.SIMPLE_CAUSE_CONFIG);
      
      StrategyTestUtil.checkChanges(changes);
      
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      final double expectedT = new TTest().t(nodeStatistic.getStatisticsOld(), nodeStatistic.getStatisticsCurrent());
      Assert.assertEquals(expectedT, nodeStatistic.getTvalue(), 0.01);
   }
   
   @Test
   public void testCauseSearchingConstantLevels()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      buildRoots();
      
      CauseSearcherConfig config = new CauseSearcherConfig(new TestCase("Test#test"), 
            true, false, 0.1, 
            false, true, RCAStrategy.LEVELWISE, 2);

      searchChanges(config);
      
      StrategyTestUtil.checkChanges(changes);
      
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      final double expectedT = new TTest().t(nodeStatistic.getStatisticsOld(), nodeStatistic.getStatisticsCurrent());
      Assert.assertEquals(expectedT, nodeStatistic.getTvalue(), 0.01);
   }
   
   @Test
   public void testWarmup()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      measurementConfig = new MeasurementConfiguration(5, TestConstants.V2, TestConstants.V1);
      measurementConfig.setWarmup(500);
      measurementConfig.setIterations(5);

      builderPredecessor = new TreeBuilder(measurementConfig);
      buildRoots();

      searchChanges(TestConstants.SIMPLE_CAUSE_CONFIG);

      StrategyTestUtil.checkChanges(changes);
      
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      final double expectedT = new TTest().t(nodeStatistic.getStatisticsOld(), nodeStatistic.getStatisticsCurrent());
      System.out.println(nodeStatistic.getMeanCurrent());
      System.out.println(expectedT + " " + nodeStatistic.getTvalue());
      // Assert.assertEquals(nodeStatistic.getMeanCurrent());
      Assert.assertEquals(expectedT, nodeStatistic.getTvalue(), 0.01);
      Assert.assertEquals(95, nodeStatistic.getMeanCurrent(), 0.01);
      Assert.assertEquals(105, nodeStatistic.getMeanOld(), 0.01);
   }

   @Test
   public void testOutlier() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      measurementConfig = new MeasurementConfiguration(30, TestConstants.V2, TestConstants.V1);
      measurementConfig.setWarmup(5);
      measurementConfig.setIterations(5);
      
      builderPredecessor = new TreeBuilder(measurementConfig);
      builderPredecessor.setAddOutlier(true);
      buildRoots();
      
      searchChanges(TestConstants.SIMPLE_CAUSE_CONFIG);
      
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      Assert.assertEquals(94, nodeStatistic.getMeanCurrent(), 0.01);
      Assert.assertEquals(104, nodeStatistic.getMeanOld(), 0.01);
      
   }
   
   private void searchChanges(final CauseSearcherConfig config) throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final CauseTester measurer = Mockito.mock(CauseTester.class);
      CauseTesterMockUtil.mockMeasurement(measurer, builderPredecessor);
      CauseSearchFolders folders = new CauseSearchFolders(folder);
      final CauseSearcher searcher = new LevelCauseSearcher(treeReader, config, measurer, measurementConfig,
            folders, new EnvironmentVariables());

      changes = searcher.search();
      data = searcher.getRCAData();
   }

}
