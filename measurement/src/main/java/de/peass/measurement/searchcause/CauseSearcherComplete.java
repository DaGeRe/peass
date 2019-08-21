package de.peass.measurement.searchcause;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

class TreeAnalyzer {
   private final List<CallTreeNode> treeStructureDiffering = new LinkedList<>();
   private final List<CallTreeNode> nonDifferingVersion = new LinkedList<>();
   private final List<CallTreeNode> nonDifferingPredecessor = new LinkedList<>();

   public TreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor) {
      getAllNodes(root, rootPredecessor);
   }

   private void getAllNodes(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      if (current.getKiekerPattern().equals(currentPredecessor.getKiekerPattern()) &&
            currentPredecessor.getChildren().size() == current.getChildren().size()) {
         nonDifferingVersion.add(current);
         nonDifferingPredecessor.add(currentPredecessor);
         compareEqualChilds(current, currentPredecessor);
      } else {
         treeStructureDiffering.add(currentPredecessor);
      }
   }

   private void compareEqualChilds(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      final Iterator<CallTreeNode> predecessorIterator = currentPredecessor.getChildren().iterator();
      final Iterator<CallTreeNode> currentIterator = current.getChildren().iterator();
      boolean oneHasNext = currentIterator.hasNext() && predecessorIterator.hasNext();
      while (oneHasNext) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         final CallTreeNode currentVersionNode = currentIterator.next();
         getAllNodes(currentVersionNode, currentPredecessorNode);
         oneHasNext = currentIterator.hasNext() && predecessorIterator.hasNext();
      }
   }

   public List<CallTreeNode> getTreeStructureDiffering() {
      return treeStructureDiffering;
   }

   public List<CallTreeNode> getNonDifferingPredecessor() {
      return nonDifferingPredecessor;
   }

   public List<CallTreeNode> getNonDifferingVersion() {
      return nonDifferingVersion;
   }
}

/**
 * Searches differing nodes in a complete call tree (instead of level-wise analysis)
 * @author reichelt
 *
 */
public class CauseSearcherComplete extends CauseSearcher {

   public CauseSearcherComplete(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final LevelMeasurer measurer, final MeasurementConfiguration measurementConfig,
         final PeASSFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
   }

   @Override
   protected List<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final TreeAnalyzer analyzer = new TreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
      final List<CallTreeNode> currentPredecessorNodeList = analyzer.getNonDifferingPredecessor();
      final List<CallTreeNode> currentVersionNodeList = analyzer.getNonDifferingVersion();
      final AllCauseSearcher levelSearcher = new AllCauseSearcher(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig, measurementConfig);

      measurer.measureVersion(currentPredecessorNodeList);
      levelSearcher.calculateDiffering();

      for (final CallTreeNode predecessorNode : currentPredecessorNodeList) {
         data.addDiff(predecessorNode);
         levelSearcher.analyseNode(predecessorNode);
      }

      differingNodes.addAll(levelSearcher.getMeasurementDiffering());
      differingNodes.addAll(analyzer.getTreeStructureDiffering());

      writeTreeState();

      return convertToChangedEntitites();
   }
}
