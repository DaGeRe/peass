package de.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.measurement.rca.CausePersistenceManager;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.BothTreeReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class LevelCauseSearcher extends CauseSearcher {
   
   /**
    * Continues existing run
    * @param measurer
    * @param finishedData
    * @param folders
    * @throws InterruptedException
    * @throws IOException
    */
   public LevelCauseSearcher(final CauseTester measurer, final CauseSearchData finishedData, final CauseSearchFolders folders) throws InterruptedException, IOException {
      super(null, finishedData.getCauseConfig(), measurer, finishedData.getMeasurementConfig(), folders);
      persistenceManager = new CausePersistenceManager(finishedData, folders);
   }
   
   public LevelCauseSearcher(final BothTreeReader reader, final CauseSearcherConfig causeSearchConfig, final CauseTester measurer, final MeasurementConfiguration measurementConfig,
         final CauseSearchFolders folders) throws InterruptedException, IOException {
      super(reader, causeSearchConfig, measurer, measurementConfig, folders);
      persistenceManager = new CausePersistenceManager(causeSearchConfig, measurementConfig, folders);

      final File potentialOldFolder = new File(folders.getArchiveResultFolder(measurementConfig.getVersion(), causeSearchConfig.getTestCase()), "0");
      if (potentialOldFolder.exists()) {
         throw new RuntimeException("Old measurement folder " + potentialOldFolder.getAbsolutePath() + " exists - please cleanup!");
      }
      new FolderDeterminer(folders).testResultFolders(measurementConfig.getVersion(), measurementConfig.getVersionOld(), causeSearchConfig.getTestCase());
   }
   
   protected Set<ChangedEntity> searchCause()
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      reader.getRootPredecessor().setOtherVersionNode(reader.getRootVersion());
      reader.getRootVersion().setOtherVersionNode(reader.getRootPredecessor());
      isLevelDifferent(Arrays.asList(new CallTreeNode[] { reader.getRootPredecessor() }),
            Arrays.asList(new CallTreeNode[] { reader.getRootVersion() }));

      return convertToChangedEntitites();
   }
}
