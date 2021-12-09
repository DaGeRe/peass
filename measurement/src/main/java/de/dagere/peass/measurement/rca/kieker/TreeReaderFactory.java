package de.dagere.peass.measurement.rca.kieker;

import java.io.File;
import java.io.IOException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.GitUtils;

public class TreeReaderFactory {

   /**
    * Creates a TreeReader and initializes the project folder used for it by cloning the given project
    * 
    * @param parentFolders
    * @param predecessor
    * @param timeout
    * @return
    * @throws InterruptedException
    * @throws IOException
    */
   public static TreeReader createTreeReader(final PeassFolders parentFolders, final String predecessor, final MeasurementConfig config, final boolean ignoreEOIs,
         final EnvironmentVariables env) throws InterruptedException, IOException {
      PeassFolders treeReadingFolders = parentFolders.getTempFolder("tree_" + predecessor);
      GitUtils.goToTag(predecessor, treeReadingFolders.getProjectFolder());
      TreeReader reader = new TreeReader(treeReadingFolders, config, env);
      reader.setIgnoreEOIs(ignoreEOIs);
      return reader;
   }

   /**
    * Creates a TreeReader directly on the given folder - only used for testing
    * 
    * @param projectFolder
    * @param predecessor
    * @param timeout
    * @return
    * @throws InterruptedException
    * @throws IOException
    */
   public static TreeReader createTestTreeReader(final File projectFolder, final MeasurementConfig measurementConfig,
         final EnvironmentVariables env) throws InterruptedException, IOException {
      TreeReader reader = new TreeReader(new PeassFolders(projectFolder), measurementConfig, env);
      return reader;
   }
}
