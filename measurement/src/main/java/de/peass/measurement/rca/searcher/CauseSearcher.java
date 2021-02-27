package de.peass.measurement.rca.searcher;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CausePersistenceManager;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.treeanalysis.AllDifferingDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

public abstract class CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcher.class);

   // Basic config
   protected final CauseSearchFolders folders;
   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfiguration measurementConfig;

   // Classes doing the real work
   protected final BothTreeReader reader;
   protected final CauseTester measurer;

   // Result
   protected List<CallTreeNode> differingNodes = new LinkedList<>();
   protected CausePersistenceManager persistenceManager;

   public CauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer, final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders)
         throws InterruptedException, IOException {
      this.reader = reader;
      this.measurer = measurer;
      this.measurementConfig = measurementConfig;
      this.folders = folders;
      this.causeSearchConfig = causeSearchConfig;

   }

   public Set<ChangedEntity> search()
         throws IOException, XmlPullParserException, InterruptedException, IllegalStateException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      reader.readTrees();

      LOG.info("Tree size: {}", reader.getRootPredecessor().getTreeSize());

      return searchCause();
   }

   protected abstract Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException;

   protected Set<ChangedEntity> convertToChangedEntitites() {
      final Set<ChangedEntity> changed = new TreeSet<>();
      differingNodes.forEach(node -> {
         if (node.getCall().equals(CauseSearchData.ADDED)) {
            changed.add(node.getOtherVersionNode().toEntity());
         } else {
            changed.add(node.toEntity());
         }
      });
      return changed;
   }

   protected void measureDefinedTree(final List<CallTreeNode> includableNodes) throws IOException, XmlPullParserException, InterruptedException,
         ViewNotFoundException, AnalysisConfigurationException, JAXBException, JsonGenerationException, JsonMappingException {
      final AllDifferingDeterminer allSearcher = new AllDifferingDeterminer(includableNodes, causeSearchConfig, measurementConfig);
      measurer.measureVersion(includableNodes);
      allSearcher.calculateDiffering();

      persistenceManager.addMeasurement(reader.getRootPredecessor());
      addMeasurements(includableNodes, reader.getRootPredecessor());

      differingNodes.addAll(allSearcher.getCurrentLevelDifferent());

      writeTreeState();
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


   protected void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      persistenceManager.writeTreeState();
   }

   public CauseSearchData getRCAData() {
      return persistenceManager.getRCAData();
   }

}
