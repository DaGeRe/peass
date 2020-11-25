package de.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
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

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.measurement.rca.CausePersistenceManager;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;
import de.peass.testtransformation.JUnitTestTransformer;
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

   protected abstract Set<ChangedEntity> searchCause() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException;

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

   protected void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      persistenceManager.writeTreeState();
   }

   public void isLevelDifferent(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final LevelDifferentNodeDeterminer levelSearcher = new LevelDifferentNodeDeterminer(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig, measurementConfig);

      final List<CallTreeNode> measurePredecessor = levelSearcher.getMeasurePredecessor();

      LOG.info("Measure next level: {}", measurePredecessor);
      if (measurePredecessor.size() > 0) {
         analyseLevel(levelSearcher, measurePredecessor);
         writeTreeState();

         isLevelDifferent(levelSearcher.getMeasureNextLevelPredecessor(), levelSearcher.getMeasureNextLevel());
      }
   }

   private void analyseLevel(final LevelDifferentNodeDeterminer levelSearcher, final List<CallTreeNode> measuredPredecessor)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      measurer.measureVersion(measuredPredecessor);
      levelSearcher.calculateDiffering();

      for (final CallTreeNode predecessorNode : measuredPredecessor) {
         persistenceManager.addMeasurement(predecessorNode);
      }

      differingNodes.addAll(levelSearcher.getTreeStructureDifferingNodes());
      differingNodes.addAll(levelSearcher.getCurrentLevelDifferent());
   }

   public CauseSearchData getRCAData() {
      return persistenceManager.getRCAData();
   }

}
