package de.dagere.peass.folders;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class CauseSearchFolders extends PeassFolders {

   private final File levelFolder;
   private final File archivedFolder;
   private final File treeFolder;
   private final File treeCacheFolder;
   private final File rcaFolder;

   public CauseSearchFolders(final File folder) {
      super(folder);
      rcaFolder = new File(peassFolder, "rca");
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
         final File valueReadingFile = new File(peassFolder, "rca" + File.separator + "getIntermediaryValues.sh");
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

   public File getArchiveResultFolder(final String commit, final TestCase testcase) {
      final File folder = new File(archivedFolder, commit + File.separator + testcase.getClazz() + File.separator + testcase.getMethod());
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
   
   public File getRcaFolder() {
      return rcaFolder;
   }

   public File getRcaTreeFolder(final String commit, final TestCase testcase) {
      final File treeDataFolder = new File(treeFolder, commit + File.separator + testcase.getShortClazz());
      return treeDataFolder;
   }

   public File getRcaTreeFile(final String commit, final TestCase testcase) {
      final File treeDataFolder = getRcaTreeFolder(commit, testcase);
      File treeFile = new File(treeDataFolder, testcase.getMethodWithParams() + ".json");
      return treeFile;
   }

   public File getRcaTreeFileDetails(final String commit, final TestCase testcase) {
      final File treeDataFolder = getRcaTreeFolder(commit, testcase);
      File treeFile = new File(treeDataFolder, "details" + File.separator + testcase.getMethodWithParams() + ".json");
      return treeFile;
   }

   public File getTreeCacheFolder(final String commit, final TestCase testcase) {
      final File folder = new File(treeCacheFolder, commit + File.separator + testcase.getClazz() + File.separator + testcase.getMethodWithParams());
      folder.mkdirs();
      return folder;
   }

}
