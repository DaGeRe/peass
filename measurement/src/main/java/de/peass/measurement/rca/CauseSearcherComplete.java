package de.peass.measurement.rca;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.treeanalysis.AllDifferingDeterminer;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Searches differing nodes in a complete call tree (instead of level-wise analysis)
 * 
 * @author reichelt
 *
 */
public class CauseSearcherComplete extends CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcherComplete.class);

   public CauseSearcherComplete(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer,
         final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
   }

   @Override
   protected Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
//      measurer.setConsiderNodePosition(true);
      final List<CallTreeNode> includableNodes;
      final CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
      final List<CallTreeNode> currentPredecessorNodeList = analyzer.getNonDifferingPredecessor();
      final List<CallTreeNode> currentVersionNodeList = analyzer.getNonDifferingVersion();
      if (causeSearchConfig.useCalibrationRun()) {
         includableNodes = getAnalysableNodes(currentPredecessorNodeList, currentVersionNodeList);
      } else {
         includableNodes = currentPredecessorNodeList;
      }

      LOG.debug("Analyzable: {} / {}", includableNodes.size(), currentPredecessorNodeList.size());

      final AllDifferingDeterminer allSearcher = new AllDifferingDeterminer(includableNodes, causeSearchConfig, measurementConfig);
      measurer.measureVersion(includableNodes);
      allSearcher.calculateDiffering();

      for (final CallTreeNode predecessorNode : includableNodes) {
         persistenceManager.addMeasurement(predecessorNode);
      }

      differingNodes.addAll(allSearcher.getCurrentLevelDifferent());
      differingNodes.addAll(analyzer.getTreeStructureDiffering());

      writeTreeState();

      return convertToChangedEntitites();
   }

   private List<CallTreeNode> getAnalysableNodes(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final MeasurementConfiguration config = new MeasurementConfiguration(1, measurementConfig.getVersion(), measurementConfig.getVersionOld());
      config.setIterations(measurementConfig.getIterations());
      config.setRepetitions(measurementConfig.getRepetitions());
      config.setWarmup(measurementConfig.getWarmup());
      config.setUseKieker(true);
      final CauseTester calibrationMeasurer = new CauseTester(folders, new JUnitTestTransformer(folders.getProjectFolder(), config), causeSearchConfig);
      final AllDifferingDeterminer calibrationRunner = new AllDifferingDeterminer(currentPredecessorNodeList, causeSearchConfig, config);
      calibrationMeasurer.measureVersion(currentPredecessorNodeList);
      final List<CallTreeNode> includableByMinTime = calibrationRunner.getIncludableNodes();
      return includableByMinTime;
   }
}
