package de.dagere.peass.measurement.rca.kieker;

import java.io.File;
import java.io.IOException;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.vcs.GitUtils;

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
   public static TreeReader createTreeReader(final PeASSFolders parentFolders, final String predecessor, final MeasurementConfiguration config, final boolean ignoreEOIs, final EnvironmentVariables env) throws InterruptedException, IOException {
      PeASSFolders treeReadingFolders = parentFolders.getTempFolder("tree_" + predecessor);
      GitUtils.goToTag(predecessor, treeReadingFolders.getProjectFolder());
      TreeReader reader = new TreeReader(treeReadingFolders, config, env);
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
   public static TreeReader createTestTreeReader(final File projectFolder, final int timeout, final EnvironmentVariables env
          ) throws InterruptedException, IOException {
      final MeasurementConfiguration config = new MeasurementConfiguration(1, timeout);
      TreeReader reader = new TreeReader(new PeASSFolders(projectFolder), config, env);
      return reader;
   }
}
