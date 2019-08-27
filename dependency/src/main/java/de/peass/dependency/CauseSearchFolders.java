package de.peass.dependency;

import java.io.File;

public class CauseSearchFolders extends PeASSFolders{

   private final File causeSearchmeasurementsFolder;
   private final File rcaFolder;
   
   public CauseSearchFolders(final File folder) {
      super(folder);
      causeSearchmeasurementsFolder = new File(fullResultFolder, "causeSearchMeasurements");
      causeSearchmeasurementsFolder.mkdir();
      rcaFolder = new File(peassFolder, "rootCauseAnalysisTree");
      rcaFolder.mkdir();
   }
   
   @Override
   public File getDetailResultFolder() {
      return causeSearchmeasurementsFolder;
   }
   
   public File getRcaFolder() {
      return rcaFolder;
   }

}
