package de.peass.measurement.searchcause;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import de.peass.measurement.searchcause.treeanalysis.AllDifferingDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Searches differing nodes in a complete call tree (instead of level-wise analysis)
 * @author reichelt
 *
 */
public class CauseSearcherComplete extends CauseSearcher {

   public CauseSearcherComplete(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final LevelMeasurer measurer, final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
   }

   @Override
   protected List<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
      final List<CallTreeNode> currentPredecessorNodeList = analyzer.getNonDifferingPredecessor();
      final List<CallTreeNode> currentVersionNodeList = analyzer.getNonDifferingVersion();
      final AllDifferingDeterminer levelSearcher = new AllDifferingDeterminer(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig, measurementConfig);

      measurer.measureVersion(currentPredecessorNodeList);
      levelSearcher.calculateDiffering();

      for (final CallTreeNode predecessorNode : currentPredecessorNodeList) {
         dataManager.addDiff(predecessorNode);
         levelSearcher.analyseNode(predecessorNode);
      }

      differingNodes.addAll(levelSearcher.getMeasurementDiffering());
      differingNodes.addAll(analyzer.getTreeStructureDiffering());

      writeTreeState();

      return convertToChangedEntitites();
   }
}
