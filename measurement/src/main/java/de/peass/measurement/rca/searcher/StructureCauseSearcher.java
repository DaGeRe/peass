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
import de.peass.measurement.rca.analyzer.StructureChangeTreeAnalyzer;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.treeanalysis.AllDifferingDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class StructureCauseSearcher extends CauseSearcher {
   
   private static final Logger LOG = LogManager.getLogger(StructureCauseSearcher.class);

   public StructureCauseSearcher(BothTreeReader reader, CauseSearcherConfig causeSearchConfig, CauseTester measurer, MeasurementConfiguration measurementConfig,
         CauseSearchFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);
   }

   @Override
   protected Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final StructureChangeTreeAnalyzer analyzer = new StructureChangeTreeAnalyzer(reader.getRootVersion(), reader.getRootPredecessor());
      final List<CallTreeNode> predecessorNodeList = analyzer.getMeasurementNodesPredecessor();

      measureDefinedTree(predecessorNodeList);

      return convertToChangedEntitites();
   }

}
