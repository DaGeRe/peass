package de.dagere.peass.measurement.rca.searcher;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.RCAMeasurementAdder;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.treeanalysis.AllDifferingDeterminer;

public abstract class CauseSearcher {

   private static final Logger LOG = LogManager.getLogger(CauseSearcher.class);

   // Basic config
   protected final CauseSearchFolders folders;
   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfig measurementConfig;
   protected final EnvironmentVariables env;

   // Classes doing the real work
   protected final BothTreeReader reader;
   protected final CauseTester measurer;

   // Result
   protected List<CallTreeNode> differingNodes = new LinkedList<>();
   protected CausePersistenceManager persistenceManager;

   public CauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer, final MeasurementConfig measurementConfig,
         final CauseSearchFolders folders, final EnvironmentVariables env) {
      this.reader = reader;
      this.measurer = measurer;
      this.measurementConfig = measurementConfig;
      this.folders = folders;
      this.causeSearchConfig = causeSearchConfig;
      this.env = env;

   }

   public Set<MethodCall> search() {
      reader.readTrees();

      LOG.info("Tree size: {}", reader.getRootPredecessor().getTreeSize());

      return searchCause();
   }

   protected abstract Set<MethodCall> searchCause();

   protected Set<MethodCall> convertToChangedEntitites() {
      final Set<MethodCall> changed = new TreeSet<>();
      differingNodes.forEach(node -> {
         changed.add(node.toEntity());
      });
      return changed;
   }

   protected void measureDefinedTree(final List<CallTreeNode> includableNodes) {
      final AllDifferingDeterminer allSearcher = new AllDifferingDeterminer(includableNodes, causeSearchConfig, measurementConfig);
      measurer.measureVersion(includableNodes);
      allSearcher.calculateDiffering();

      RCAMeasurementAdder measurementReader = new RCAMeasurementAdder(persistenceManager, includableNodes);
      measurementReader.addAllMeasurements(reader.getRootPredecessor());
      
      differingNodes.addAll(allSearcher.getLevelDifferentPredecessor());

      writeTreeState();
   }

   protected void writeTreeState() {
      persistenceManager.writeTreeState();
   }

   public CauseSearchData getRCAData() {
      return persistenceManager.getRCAData();
   }

}
