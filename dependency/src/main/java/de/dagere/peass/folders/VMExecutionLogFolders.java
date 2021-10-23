package de.dagere.peass.folders;

import java.io.File;

public class VMExecutionLogFolders {
   private final File dependencyLogFolder, measureLogFolder, treeLogFolder, rcaLogFolder;
   
   public VMExecutionLogFolders(final File peassFolder) {
      dependencyLogFolder = new File(peassFolder, "logs/dependencyLogs");
      measureLogFolder = new File(peassFolder, "logs/measureLogs");
      treeLogFolder = new File(peassFolder, "logs/treeLogs");
      rcaLogFolder = new File(peassFolder, "logs/rcaLogs");
   }
   
   public File getDependencyLogFolder() {
      if (!dependencyLogFolder.exists()) {
         dependencyLogFolder.mkdirs();
      }
      return dependencyLogFolder;
   }
   
   public File getMeasureLogFolder() {
      if (!measureLogFolder.exists()) {
         measureLogFolder.mkdirs();
      }
      return measureLogFolder;
   }
   
   public File getTreeLogFolder() {
      if (!treeLogFolder.exists()) {
         treeLogFolder.mkdirs();
      }
      return treeLogFolder;
   }
   
   public File getRCALogFolder() {
      if (!rcaLogFolder.exists()) {
         rcaLogFolder.mkdirs();
      }
      return rcaLogFolder;
   }
}
