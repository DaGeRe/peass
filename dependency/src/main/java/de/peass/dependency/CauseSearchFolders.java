package de.peass.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

      makeClearscript();
   }

   private void makeClearscript() {
      final File script = new File(peassFolder, "clearRCA.sh");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(script))) {
         writer.write("rm rca/level -rf && rm rca/archived -rf && rm rca/tree -rf\n");
         writer.write("rm measurementsTemp -rf\n");
         writer.write("rm kiekerTemp -rf\n");
         writer.write("rm projectTemp -rf\n");
         writer.write("rm temp -rf\n");
         writer.write("rm logs -rf");
      } catch (final IOException e) {
         e.printStackTrace();
      }
      script.setExecutable(true);
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
