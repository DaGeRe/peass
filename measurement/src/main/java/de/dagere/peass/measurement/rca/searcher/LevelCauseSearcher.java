package de.dagere.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.measurement.organize.FolderDeterminer;
import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.treeanalysis.LevelDifferentNodeDeterminer;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependencyprocessors.ViewNotFoundException;
import kieker.analysis.exception.AnalysisConfigurationException;

public class LevelCauseSearcher extends CauseSearcher {
   
   private static final Logger LOG = LogManager.getLogger(LevelCauseSearcher.class);
   
   /**
    * Continues existing run
    * @param measurer
    * @param finishedData
    * @param folders
    * @throws InterruptedException
    * @throws IOException
    */
   public LevelCauseSearcher(final CauseTester measurer, final CauseSearchData finishedData, final CauseSearchFolders folders, final EnvironmentVariables env) throws InterruptedException, IOException {
      super(null, finishedData.getCauseConfig(), measurer, finishedData.getMeasurementConfig(), folders, env);
      persistenceManager = new CausePersistenceManager(finishedData, folders);
   }
   
   public LevelCauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer, final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders, final EnvironmentVariables env) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders, env);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);

      final File potentialOldFolder = new File(folders.getArchiveResultFolder(measurementConfig.getVersion(), causeSearchConfig.getTestCase()), "0");
      if (potentialOldFolder.exists()) {
         throw new RuntimeException("Old measurement folder " + potentialOldFolder.getAbsolutePath() + " exists - please cleanup!");
      }
      new FolderDeterminer(folders).testResultFolders(measurementConfig.getVersion(), measurementConfig.getVersionOld(), causeSearchConfig.getTestCase());
   }
   
   @Override
   protected Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      reader.getRootPredecessor().setOtherVersionNode(reader.getRootVersion());
      reader.getRootVersion().setOtherVersionNode(reader.getRootPredecessor());
      isLevelDifferent(Arrays.asList(new CallTreeNode[] { reader.getRootPredecessor() }),
            Arrays.asList(new CallTreeNode[] { reader.getRootVersion() }));

      return convertToChangedEntitites();
   }
   
   public void isLevelDifferent(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final LevelDifferentNodeDeterminer levelSearcher = new LevelDifferentNodeDeterminer(currentPredecessorNodeList, currentVersionNodeList, causeSearchConfig, measurementConfig);

      final List<CallTreeNode> measurePredecessor = levelSearcher.getMeasurePredecessor();

      LOG.info("Measure next level: {}", measurePredecessor);
      if (measurePredecessor.size() > 0) {
         measureLevel(levelSearcher, measurePredecessor);
         writeTreeState();

         isLevelDifferent(levelSearcher.getMeasureNextLevelPredecessor(), levelSearcher.getMeasureNextLevel());
      }
   }

   private void measureLevel(final LevelDifferentNodeDeterminer levelSearcher, final List<CallTreeNode> measuredPredecessor)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      measurer.measureVersion(measuredPredecessor);
      levelSearcher.calculateDiffering();

      for (final CallTreeNode predecessorNode : measuredPredecessor) {
         persistenceManager.addMeasurement(predecessorNode);
      }

      differingNodes.addAll(levelSearcher.getTreeStructureDifferingNodes());
      differingNodes.addAll(levelSearcher.getCurrentLevelDifferent());
   }
}
