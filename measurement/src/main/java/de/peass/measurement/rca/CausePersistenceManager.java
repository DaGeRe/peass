package de.peass.measurement.rca;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.utils.Constants;

public class CausePersistenceManager {

   protected final CauseSearchData data;
   protected final CauseSearchData dataDetails;
   private final File treeDataFile;
   private final File treeDataFileDetails;

   public CausePersistenceManager(final CauseSearcherConfig causeSearchConfig, final MeasurementConfiguration measurementConfig, final CauseSearchFolders folders) {
      this(new CauseSearchData(measurementConfig, causeSearchConfig), folders);
   }

   public CausePersistenceManager(final CauseSearchData finishedData, final CauseSearchFolders folders) {
      this.data = finishedData;
      this.dataDetails = new CauseSearchData(finishedData.getMeasurementConfig(), finishedData.getCauseConfig());
      if (data.getNodes() != null) {
         dataDetails.setNodes(data.getNodes());
      }

      final File treeDataFolder = new File(folders.getRcaTreeFolder(), finishedData.getMeasurementConfig().getVersion() + File.separator +
            finishedData.getCauseConfig().getTestCase().getShortClazz());
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
}
