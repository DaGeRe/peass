package de.peass.dependency;

import java.io.File;

public class CauseSearchFolders extends PeASSFolders{

   private final File causeSearchmeasurementsFolder;
   
   public CauseSearchFolders(final File folder) {
      super(folder);
      causeSearchmeasurementsFolder = new File(fullResultFolder, "causeSearchMeasurements");
      causeSearchmeasurementsFolder.mkdir();
   }
   
   @Override
   public File getDetailResultFolder() {
      return causeSearchmeasurementsFolder;
   }

}
