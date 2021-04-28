package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Ignore;
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
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.TreeBuilderLeafs;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Tests whether merging leaf data of results can be done - will be eventually used in the future, currently, merging should be done using
 * KoPeMes ignoreEOI
 * @author reichelt
 *
 */
@Ignore
public class CauseSearcherMergeTest {

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
   public void testMerging()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CallTreeNode root1 = new TreeBuilderLeafs().getRoot();
      final CallTreeNode root2 = new TreeBuilderLeafs().getRoot();

      cleanup();
      final File folder = new File("target/test/");
      folder.mkdir();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);
      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);

      final CauseTester measurer = Mockito.mock(CauseTester.class);
      final CauseSearcher searcher = new LevelCauseSearcher(treeReader, 
            new CauseSearcherConfig(new TestCase("Test#test"), true, false, 0.1, false, true, RCAStrategy.COMPLETE),
            measurer, measurementConfig, new CauseSearchFolders(folder), new EnvironmentVariables());

      final Set<ChangedEntity> changes = searcher.search();

      System.out.println(changes);
      Assert.assertEquals(1, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.iterator().next().toString());

      final CauseSearchData data = searcher.getRCAData();
      final TestcaseStatistic nodeStatistic = data.getNodes().getStatistic();
      final double expectedT = new TTest().t(nodeStatistic.getStatisticsOld(), nodeStatistic.getStatisticsCurrent());
      Assert.assertEquals(expectedT, nodeStatistic.getTvalue(), 0.01);
   }

}
