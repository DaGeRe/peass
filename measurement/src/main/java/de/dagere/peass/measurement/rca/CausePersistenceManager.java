package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;

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

      final File treeDataFolder = folders.getRcaTreeFolder(finishedData.getMeasurementConfig().getExecutionConfig().getVersion(), finishedData.getCauseConfig().getTestCase());
      treeDataFile = new File(treeDataFolder, finishedData.getCauseConfig().getTestCase().getMethod() + ".json");
      if (treeDataFile.exists()) {
         throw new RuntimeException("Old tree data folder " + treeDataFile.getAbsolutePath() + " exists - please cleanup!");
      }
      treeDataFolder.mkdirs();
      treeDataFileDetails = new File(treeDataFolder, "details" + File.separator + finishedData.getCauseConfig().getTestCase().getMethod() + ".json");
      treeDataFileDetails.getParentFile().mkdirs();
   }

   public void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      Constants.OBJECTMAPPER.writeValue(treeDataFile, data);
      Constants.OBJECTMAPPER.writeValue(treeDataFileDetails, dataDetails);
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
