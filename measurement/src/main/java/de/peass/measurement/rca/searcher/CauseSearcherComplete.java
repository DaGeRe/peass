package de.peass.measurement.rca.searcher;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CausePersistenceManager;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.analyzer.CompleteTreeAnalyzer;
import de.peass.measurement.rca.analyzer.TreeAnalyzer;
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

   private final TreeAnalyzerCreator creator;

   public CauseSearcherComplete(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer,
         final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);
      creator = new TreeAnalyzerCreator() {
         @Override
         public TreeAnalyzer getAnalyzer(BothTreeReader reader, CauseSearcherConfig config) {
            return new CompleteTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
         }
      };
   }
   
   public CauseSearcherComplete(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer,
         final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders, TreeAnalyzerCreator creator) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);
      this.creator = creator;
   }

   @Override
   protected Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final TreeAnalyzer analyzer = creator.getAnalyzer(reader, causeSearchConfig);
      final List<CallTreeNode> predecessorNodeList = analyzer.getMeasurementNodesPredecessor();
      final List<CallTreeNode> includableNodes = getIncludableNodes(predecessorNodeList);

      measureDefinedTree(includableNodes);
//      differingNodes.addAll(analyzer.getTreeStructureDiffering());

      return convertToChangedEntitites();
   }

   private List<CallTreeNode> getIncludableNodes(final List<CallTreeNode> predecessorNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final List<CallTreeNode> includableNodes;
      if (causeSearchConfig.useCalibrationRun()) {
         includableNodes = getAnalysableNodes(predecessorNodeList);
      } else {
         includableNodes = predecessorNodeList;
      }

      LOG.debug("Analyzable: {} / {}", includableNodes.size(), predecessorNodeList.size());
      return includableNodes;
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
