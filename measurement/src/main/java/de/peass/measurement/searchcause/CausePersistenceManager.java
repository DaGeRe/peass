package de.peass.measurement.searchcause;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.data.CauseSearchData;
import de.peass.utils.Constants;

public class CausePersistenceManager {

   protected final CauseSearchData data;
   protected final CauseSearchData dataDetails;
   private final File treeDataFile;
   private final File treeDataFileDetails;

   public CausePersistenceManager(final CauseSearcherConfig causeSearchConfig, final MeasurementConfiguration measurementConfig, final CauseSearchFolders folders) {
      data = new CauseSearchData(causeSearchConfig.getTestCase(), measurementConfig.getVersion(), measurementConfig.getVersionOld(), measurementConfig);
      dataDetails = new CauseSearchData(causeSearchConfig.getTestCase(), measurementConfig.getVersion(), measurementConfig.getVersionOld(), measurementConfig);

      final File treeDataFolder = new File(folders.getRcaTreeFolder(), measurementConfig.getVersion() + File.separator +
            causeSearchConfig.getTestCase().getShortClazz());
      treeDataFile = new File(treeDataFolder, causeSearchConfig.getTestCase().getMethod() + ".json");
      if (treeDataFile.exists()) {
         throw new RuntimeException("Old tree data folder " + treeDataFile.getAbsolutePath() + " exists - please cleanup!");
      }
      treeDataFolder.mkdirs();
      treeDataFileDetails = new File(treeDataFolder, "details" + File.separator + causeSearchConfig.getTestCase().getMethod() + ".json");
      treeDataFileDetails.getParentFile().mkdirs();
   }
   
   protected void writeTreeState() throws IOException, JsonGenerationException, JsonMappingException {
      Constants.OBJECTMAPPER.writeValue(treeDataFile, data);
      Constants.OBJECTMAPPER.writeValue(treeDataFileDetails, dataDetails);
   }

   public void addMeasurement(final CallTreeNode predecessorNode) {
      data.addDiff(predecessorNode);
      dataDetails.addDetailDiff(predecessorNode);
   }
}
