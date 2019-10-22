package de.peass.measurement.rca.treeanalysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.helper.TestConstants;
import de.peass.measurement.rca.helper.TreeBuilderDifferent;
import de.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TestLevelDifferentNodeDeterminer {
   @Test
   public void testLevelDifferingDeterminer() throws Exception {
      final TreeBuilderDifferent predecessorBuilder = new TreeBuilderDifferent();
      final CallTreeNode rootOld = predecessorBuilder.getRoot();
      final CallTreeNode rootMain = predecessorBuilder.getRoot();

      final LevelDifferentNodeDeterminer determiner = getDiff(rootOld, rootMain);
      Assert.assertEquals(2, determiner.getMeasureNextLevel().size());
      
      final LevelDifferentNodeDeterminer determinerD = getDiff(predecessorBuilder.getD(), predecessorBuilder.getD());
      Assert.assertEquals(1, determinerD.getCurrentLevelDifferent().size());
   }

   private LevelDifferentNodeDeterminer getDiff(final CallTreeNode rootOld, final CallTreeNode rootMain)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final List<CallTreeNode> currentPredecessorNodeList = Arrays.asList(new CallTreeNode[] { rootOld });
      final List<CallTreeNode> currentVersionNodeList = Arrays.asList(new CallTreeNode[] { rootMain });
      final LevelDifferentNodeDeterminer determiner = new LevelDifferentNodeDeterminer(currentPredecessorNodeList, currentVersionNodeList,
            TestConstants.SIMPLE_CAUSE_CONFIG, TestConstants.SIMPLE_MEASUREMENT_CONFIG);
      determiner.calculateDiffering();
      return determiner;
   }
}
