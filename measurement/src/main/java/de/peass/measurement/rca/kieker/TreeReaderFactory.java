package de.peass.measurement.rca.kieker;

import java.io.File;
import java.io.IOException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.peass.vcs.GitUtils;

public class TreeReaderFactory {
   
   /**
    * Creates a TreeReader and initializes the project folder used for it by cloning the given project
    * @param parentFolders
    * @param predecessor
    * @param timeout
    * @return
    * @throws InterruptedException
    * @throws IOException
    */
   public static TreeReader createTreeReader(final PeASSFolders parentFolders, final String predecessor, MeasurementConfiguration config, boolean ignoreEOIs) throws InterruptedException, IOException {
      File treeReadingFolder = new File(parentFolders.getTempProjectFolder(), predecessor);
      if (treeReadingFolder.exists()) {
         
      }
      TemporaryProjectFolderUtil.cloneForcefully(parentFolders, treeReadingFolder);
      GitUtils.goToTag(predecessor, treeReadingFolder);
      TreeReader reader = new TreeReader(treeReadingFolder, config);
      reader.setIgnoreEOIs(ignoreEOIs);
      return reader;
   }
   
   /**
    * Creates a TreeReader directly on the given folder - only used for testing
    * @param projectFolder
    * @param predecessor
    * @param timeout
    * @return
    * @throws InterruptedException
    * @throws IOException
    */
   public static TreeReader createTestTreeReader(final File projectFolder, final int timeout) throws InterruptedException, IOException {
      final MeasurementConfiguration config = new MeasurementConfiguration(1, timeout);
      TreeReader reader = new TreeReader(projectFolder, config);
      return reader;
   }
}
