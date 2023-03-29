package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;

/**
 * Persists RCA data to the file system 
 *
 */
public class CausePersistenceManager {

   protected final CauseSearchData data;
   protected final CauseSearchData dataDetails;
   private final CauseSearchFolders folders;
   private final File treeDataFile;
   private final File treeDataFileDetails;

   public CausePersistenceManager(final CauseSearcherConfig causeSearchConfig, final MeasurementConfig measurementConfig, final CauseSearchFolders folders) {
      this(new CauseSearchData(measurementConfig, causeSearchConfig), new CauseSearchData(measurementConfig, causeSearchConfig), folders);
   }

   public CausePersistenceManager(final CauseSearchData finishedData, final CauseSearchData finishedDataFull, final CauseSearchFolders folders) {
      this.data = finishedData;
      this.dataDetails = finishedDataFull;
      this.folders = folders;

      String commit = finishedData.getMeasurementConfig().getFixedCommitConfig().getCommit();
      TestMethodCall testCase = finishedData.getCauseConfig().getTestCase();
      final File treeDataFolder = folders.getRcaTreeFolder(commit, testCase);
      treeDataFile = folders.getRcaTreeFile(commit, testCase);
      if (treeDataFile.exists()) {
         throw new RuntimeException("Old tree data folder " + treeDataFile.getAbsolutePath() + " exists - please cleanup!");
      }
      treeDataFolder.mkdirs();
      treeDataFileDetails = folders.getRcaTreeFileDetails(commit, testCase);
      treeDataFileDetails.getParentFile().mkdirs();
   }

   public void writeTreeState() {
      try {
         Constants.OBJECTMAPPER.writeValue(treeDataFile, data);
         Constants.OBJECTMAPPER.writeValue(treeDataFileDetails, dataDetails);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public void addMeasurement(final CallTreeNode predecessorNode) {
      data.addDiff(predecessorNode);
      dataDetails.addDetailDiff(predecessorNode);
   }

   public CauseSearchData getRCAData() {
      return data;
   }

   public CauseSearchFolders getFolders() {
      return folders;
   }
}
