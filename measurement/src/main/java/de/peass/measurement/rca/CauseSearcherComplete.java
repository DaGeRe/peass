package de.peass.measurement.rca;

import java.io.IOException;
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
      // measurer.setConsiderNodePosition(true);
      
      final CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
      final List<CallTreeNode> predecessorNodeList = analyzer.getNonDifferingPredecessor();
      final List<CallTreeNode> includableNodes;
      if (causeSearchConfig.useCalibrationRun()) {
         includableNodes = getAnalysableNodes(predecessorNodeList);
      } else {
         includableNodes = predecessorNodeList;
      }

      LOG.debug("Analyzable: {} / {}", includableNodes.size(), predecessorNodeList.size());

      final AllDifferingDeterminer allSearcher = new AllDifferingDeterminer(includableNodes, causeSearchConfig, measurementConfig);
      measurer.measureVersion(includableNodes);
      allSearcher.calculateDiffering();
      
      persistenceManager.addMeasurement(reader.getRootPredecessor());
      addMeasurements(includableNodes, reader.getRootPredecessor());

      differingNodes.addAll(allSearcher.getCurrentLevelDifferent());
      differingNodes.addAll(analyzer.getTreeStructureDiffering());

      writeTreeState();

      return convertToChangedEntitites();
   }

   private void addMeasurements(final List<CallTreeNode> includableNodes, CallTreeNode parent) {
      for (CallTreeNode child : parent.getChildren()) {
         if (includableNodes.contains(child)) {
            LOG.debug("Analyzing: {}", child);
            persistenceManager.addMeasurement(child);
            addMeasurements(includableNodes, child);
         }
      }
   }

   private List<CallTreeNode> getAnalysableNodes(final List<CallTreeNode> predecessorNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final MeasurementConfiguration config = new MeasurementConfiguration(1, measurementConfig.getVersion(), measurementConfig.getVersionOld());
      config.setIterations(measurementConfig.getIterations());
      config.setRepetitions(measurementConfig.getRepetitions());
      config.setWarmup(measurementConfig.getWarmup());
      config.setUseKieker(true);
      final CauseTester calibrationMeasurer = new CauseTester(folders, new JUnitTestTransformer(folders.getProjectFolder(), config), causeSearchConfig);
      final AllDifferingDeterminer calibrationRunner = new AllDifferingDeterminer(predecessorNodeList, causeSearchConfig, config);
      calibrationMeasurer.measureVersion(predecessorNodeList);
      final List<CallTreeNode> includableByMinTime = calibrationRunner.getIncludableNodes();
      return includableByMinTime;
   }
}
