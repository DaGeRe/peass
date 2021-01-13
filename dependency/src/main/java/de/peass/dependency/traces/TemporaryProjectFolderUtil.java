package de.peass.dependency.traces;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.PeASSFolders;
import de.peass.vcs.GitUtils;

public class TemporaryProjectFolderUtil {
   
   private static final Logger LOG = LogManager.getLogger(TemporaryProjectFolderUtil.class);
   
   public static PeASSFolders cloneForcefully(PeASSFolders originalFolders, File dest) throws IOException, InterruptedException {
      if (dest.exists()) {
         LOG.warn("Deleting existing folder {}", dest);
         FileUtils.deleteDirectory(dest);
         File peassFolder = new File(dest.getParentFile(), dest.getName() + "_peass");
         if (peassFolder.exists()) {
            FileUtils.deleteDirectory(peassFolder);
         }
      }
      GitUtils.clone(originalFolders, dest);
      final PeASSFolders folders = new PeASSFolders(dest);
      return folders;
   }
}
