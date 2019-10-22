package de.peass.measurement.rca.kieker;

import java.io.File;
import java.io.IOException;

import de.peass.dependency.PeASSFolders;
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
   public static TreeReader createTreeReader(final PeASSFolders parentFolders, final String predecessor, final int timeout) throws InterruptedException, IOException {
      File treeReadingFolder = new File(parentFolders.getTempProjectFolder(), predecessor);
      GitUtils.clone(parentFolders, treeReadingFolder);
      GitUtils.goToTag(predecessor, treeReadingFolder);
      TreeReader reader = new TreeReader(treeReadingFolder, timeout);
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
      TreeReader reader = new TreeReader(projectFolder, timeout);
      return reader;
   }
}
