package de.dagere.peass.measurement.rca.strategytest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;



import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.CauseTesterMockUtil;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.TestConstants;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.helper.TreeBuilderBig;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcherComplete;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcherCompleteTest {

   final CauseTester measurer = Mockito.mock(CauseTester.class);

   @Before
   public void cleanup() {
      final File folder = new File("target/test_peass/");
      try {
         FileUtils.deleteDirectory(folder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testSameNodesChanges() throws Exception {
      final TreeBuilderBig builderPredecessor = new TreeBuilderBig(true);
      final TreeBuilderBig builderVersion = new TreeBuilderBig(true);
      final CallTreeNode rootPredecessor = builderPredecessor.getRoot();
      final CallTreeNode rootVersion = builderVersion.getRoot();

      CauseTesterMockUtil.mockMeasurement(measurer, builderPredecessor);

      final Set<ChangedEntity> changes = getChanges(rootPredecessor, rootVersion);

      StrategyTestUtil.checkChanges(changes);
   }

   @Test
   public void testSameNodesNotDifferent() throws Exception {
      final TreeBuilderBig builderPredecessor = new TreeBuilderBig(false);
      final CallTreeNode rootPredecessor = builderPredecessor.getRoot();
      final CallTreeNode rootVersion = new TreeBuilderBig(false).getRoot();

      CauseTesterMockUtil.mockMeasurement(measurer, builderPredecessor);

      final Set<ChangedEntity> changes = getChanges(rootPredecessor, rootVersion);

      StrategyTestUtil.checkChanges(changes);
   }
   
   @Test
   public void testDifferentTree() throws Exception {
      final TreeBuilder builderPredecessor = new TreeBuilder();
      final CallTreeNode rootPredecessor = builderPredecessor.getRoot();
      final TreeBuilder differentTreeBuilder = new TreeBuilder();
      differentTreeBuilder.addDE();
      final CallTreeNode rootVersion = differentTreeBuilder.getRoot();

      CauseTesterMockUtil.mockMeasurement(measurer, builderPredecessor);

      final Set<ChangedEntity> changes = getChanges(rootPredecessor, rootVersion);

      System.out.println(changes);
      MatcherAssert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassB#methodB", "")));
//      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassD#methodD", "")));
//      Assert.assertThat(changes, Matchers.hasItem(new ChangedEntity("ClassE#methodE", "")));

      ArgumentCaptor<List<CallTreeNode>> includedNodes = ArgumentCaptor.forClass(List.class);
      Mockito.verify(measurer).measureVersion(includedNodes.capture());

      MatcherAssert.assertThat(includedNodes.getValue(), Matchers.hasItem(builderPredecessor.getB()));
      MatcherAssert.assertThat(includedNodes.getValue(), Matchers.hasItem(builderPredecessor.getC()));
   }

   private Set<ChangedEntity> getChanges(final CallTreeNode rootPredecessor, final CallTreeNode rootVersion)
         throws InterruptedException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      final File folder = new File("target/test/");
      folder.mkdir();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);

      Mockito.when(treeReader.getRootPredecessor()).thenReturn(rootPredecessor);
      Mockito.when(treeReader.getRootVersion()).thenReturn(rootVersion);

      final CauseSearcherComplete searcher = new CauseSearcherComplete(treeReader, TestConstants.SIMPLE_CAUSE_CONFIG, measurer,
            TestConstants.SIMPLE_MEASUREMENT_CONFIG,
            new CauseSearchFolders(folder), new EnvironmentVariables());
      final Set<ChangedEntity> changes = searcher.search();

      return changes;
   }

}
