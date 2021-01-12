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
   
   public static PeASSFolders cloneForcefully(PeASSFolders originalFolders, File projectFolderTemp) throws IOException, InterruptedException {
      if (projectFolderTemp.exists()) {
         LOG.warn("Deleting existing folder {}", projectFolderTemp);
         FileUtils.deleteDirectory(projectFolderTemp);
         File peassFolder = new File(projectFolderTemp.getParentFile(), projectFolderTemp.getName() + "_peass");
         if (peassFolder.exists()) {
            FileUtils.deleteDirectory(peassFolder);
         }
      }
      GitUtils.clone(originalFolders, projectFolderTemp);
      final PeASSFolders folders = new PeASSFolders(projectFolderTemp);
      return folders;
   }
}
