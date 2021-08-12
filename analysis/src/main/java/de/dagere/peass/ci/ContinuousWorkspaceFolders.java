package de.dagere.peass.ci;

import java.io.File;

import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.ResultsFolders;

public class ContinuousWorkspaceFolders {
   private final File localFolder;
   private final PeassFolders folders;
   private final ResultsFolders resultsFolders;
   
   public ContinuousWorkspaceFolders(final File localFolder, final PeassFolders folders, final ResultsFolders resultsFolders) {
      this.localFolder = localFolder;
      this.folders = folders;
      this.resultsFolders = resultsFolders;
   }
   
   
}
