package de.dagere.peass.folders;

import java.io.File;

public class TempPeassFolders extends PeassFolders {

   private final VMExecutionLogFolders parentLogFolders;
   
   public TempPeassFolders(final File folder, final String projectName, final VMExecutionLogFolders parentLogFolders) {
      super(folder, projectName);
      this.parentLogFolders = parentLogFolders;
   }
   
   @Override
   public File getDependencyLogFolder() {
      return parentLogFolders.getDependencyLogFolder();
   }
   
   @Override
   public File getMeasureLogFolder() {
      return parentLogFolders.getMeasureLogFolder();
   }
   
   @Override
   public File getTreeLogFolder() {
      return parentLogFolders.getTreeLogFolder();
   }
   
   @Override
   public File getRCALogFolder() {
      return parentLogFolders.getRCALogFolder();
   }

}
