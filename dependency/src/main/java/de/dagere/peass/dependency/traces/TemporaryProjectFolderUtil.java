package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.vcs.GitUtils;

public class TemporaryProjectFolderUtil {

   private static final Logger LOG = LogManager.getLogger(TemporaryProjectFolderUtil.class);

   public static PeassFolders cloneForcefully(final PeassFolders originalFolders, final File dest) throws IOException, InterruptedException {
      if (dest.exists()) {
         LOG.warn("Deleting existing folder {}", dest);
         FileUtils.deleteDirectory(dest);
         File peassFolder = new File(dest.getParentFile(), dest.getName() + PeassFolders.PEASS_POSTFIX);
         if (peassFolder.exists()) {
            FileUtils.deleteDirectory(peassFolder);
         }
      }
      GitUtils.clone(originalFolders, dest);
      final PeassFolders folders = new PeassFolders(dest, originalFolders.getProjectName());
      return folders;
   }
}
