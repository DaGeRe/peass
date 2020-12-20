package de.peass.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import de.peass.dependency.analysis.data.TestCase;

public class CauseSearchFolders extends PeASSFolders {

   private final File levelFolder;
   private final File archivedFolder;
   private final File treeFolder;
   private final File treeCacheFolder;

   public CauseSearchFolders(final File folder) {
      super(folder);
      final File rcaFolder = new File(peassFolder, "rca");
      levelFolder = new File(rcaFolder, "level");
      levelFolder.mkdir();
      archivedFolder = new File(rcaFolder, "archived");
      archivedFolder.mkdir();
      treeFolder = new File(rcaFolder, "tree");
      treeFolder.mkdir();
      treeCacheFolder = new File(rcaFolder, "treeCache");
      treeCacheFolder.mkdir();

      copyScripts();
   }

   private void copyScripts() {
      try {
         final URL getProgressScript = CauseSearchFolders.class.getClassLoader().getResource("copy/getProgress.sh");
         final File getProgressFile = new File(peassFolder, "getProgress.sh");
         FileUtils.copyURLToFile(getProgressScript, getProgressFile);
         getProgressFile.setExecutable(true);
         
         final URL clearRcaScript = CauseSearchFolders.class.getClassLoader().getResource("copy/clearRCA.sh");
         final File clearRcaFile = new File(peassFolder, "clearRCA.sh");
         FileUtils.copyURLToFile(clearRcaScript, clearRcaFile);
         clearRcaFile.setExecutable(true);
         
         final URL valueReadingScript = CauseSearchFolders.class.getClassLoader().getResource("copy/getIntermediaryValues.sh");
         final File valueReadingFile = new File(peassFolder, "rca/getIntermediaryValues.sh");
         FileUtils.copyURLToFile(valueReadingScript, valueReadingFile);
         valueReadingFile.setExecutable(true);
      } catch (IOException e) {
         e.printStackTrace();
      }
     
   }

   @Override
   public File getDetailResultFolder() {
      return levelFolder;
   }

   public File getArchiveResultFolder(final String version, final TestCase testcase) {
      final File folder = new File(archivedFolder, version + File.separator + testcase.getClazz() + File.separator + testcase.getMethod());
      if (!folder.exists()) {
         folder.mkdirs();
      }
      return folder;
   }

   public File getArchivedFolder() {
      return archivedFolder;
   }

   public File getRcaTreeFolder() {
      return treeFolder;
   }

   public File getTreeCacheFolder(final String version, final TestCase testcase) {
      final File folder = new File(treeCacheFolder, version + File.separator + testcase.getClazz() + File.separator + testcase.getMethod());
      folder.mkdirs();
      return folder;
   }

}
